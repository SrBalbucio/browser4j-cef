# Browser Automation API

The Browser Automation API provides a high-level, human-like interface for automating web interactions. It combines JavaScript-based element selection with native input simulation (mouse and keyboard events) to provide a reliable and low-detectability automation solution.

## Core Interface: `AutomationModule`

The automation module is accessible via `browser.automation()`.

### Common Methods

- `waitForSelector(String selector, long timeoutMs)`: Service-side wait for an element to appear in the DOM.
- `click(String selector)`: Moves the mouse to the center of the element and performs a native left-click.
- `type(String selector, String text)`: Focuses the element and simulates native key presses for each character.
- `isVisible(String selector)`: Checks if an element is present and has non-zero dimensions.
- `getAttribute(String selector, String attribute)`: Retrieves a specific attribute from an element.

## Usage Example

```java
browser.automation().waitForSelector("#login-button", 5000)
    .thenCompose(v -> browser.automation().type("#username", "myUser"))
    .thenCompose(v -> browser.automation().type("#password", "myPass"))
    .thenCompose(v -> browser.automation().click("#login-button"))
    .thenRun(() -> System.out.println("Login sequence initiated!"));
```

## Hybrid Interaction Model

1.  **Selector Engine**: Uses standard CSS selectors via `document.querySelector`.
2.  **Coordinate Calculation**: JavaScript retrieves the precise bounding box and visibility of the element.
3.  **Native Dispatch**: Java's `InputController` dispatches raw AWT/Chromium events (mouse move, mouse down, mouse up, key events) to the calculated coordinates.

This design ensures that interactions are indistinguishable from real user input, bypassing many simple bot detection scripts.
