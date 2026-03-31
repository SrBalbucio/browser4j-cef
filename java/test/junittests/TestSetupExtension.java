// Copyright (c) 2019 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package tests.junittests;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

import org.cef.CefApp;
import org.cef.CefApp.CefAppState;
import org.cef.CefSettings;
import org.cef.handler.CefAppHandlerAdapter;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.concurrent.CountDownLatch;

// All test cases must install this extension for CEF to be properly initialized
// and shut down.
//
// For example:
//
//   @ExtendWith(TestSetupExtension.class)
//   class FooTest {
//        @Test
//        void testCaseThatRequiresCEF() {}
//   }
//
// This code is based on https://stackoverflow.com/a/51556718.
public class TestSetupExtension
        implements BeforeAllCallback, ExtensionContext.Store.CloseableResource {
    private static boolean initialized_ = false;
    private static boolean cefAvailable_ = true;
    private static CountDownLatch countdown_ = new CountDownLatch(1);

    public static boolean isCefAvailable() {
        return cefAvailable_;
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        if (!initialized_) {
            initialized_ = true;
            try {
                initialize(context);
            } catch (UnsatisfiedLinkError e) {
                cefAvailable_ = false;
                System.err.println("CEF native library not available: " + e.getMessage());
            } catch (Throwable t) {
                cefAvailable_ = false;
                System.err.println("CEF initialization error: " + t.getMessage());
            }
        }
    }

    // Executed before any tests are run.
    private void initialize(ExtensionContext context) {
        TestSetupContext.initialize(context);

        if (TestSetupContext.debugPrint()) {
            System.out.println("TestSetupExtension.initialize");
        }

        // Register a callback hook for when the root test context is shut down.
        context.getRoot().getStore(GLOBAL).put("jcef_test_setup", this);

        // Perform startup initialization on platforms that require it.
        try {
            if (!CefApp.startup(null)) {
                System.out.println("Startup initialization failed!");
                cefAvailable_ = false;
                return;
            }
        } catch (UnsatisfiedLinkError e) {
            System.err.println("CEF startup failed: " + e.getMessage());
            cefAvailable_ = false;
            return;
        }

        CefApp.addAppHandler(new CefAppHandlerAdapter(null) {
            @Override
            public void stateHasChanged(org.cef.CefApp.CefAppState state) {
                if (state == CefAppState.TERMINATED) {
                    // Signal completion of CEF shutdown.
                    countdown_.countDown();
                }
            }
        });

        // Initialize the singleton CefApp instance.
        CefSettings settings = new CefSettings();
        CefApp.getInstance(settings);
    }

    // Executed after all tests have completed.
    @Override
    public void close() {
        if (TestSetupContext.debugPrint()) {
            System.out.println("TestSetupExtension.close");
        }

        if (!cefAvailable_) {
            if (TestSetupContext.debugPrint()) {
                System.out.println("Skipping CEF cleanup because native initialization failed.");
            }
            return;
        }

        CefApp.getInstance().dispose();

        // Wait for CEF shutdown to complete.
        try {
            countdown_.await();
        } catch (InterruptedException e) {
        }
    }
}
