import XCTest

final class TaskManagerUITests: XCTestCase {

    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    @MainActor
    private func launch(seeded: Bool) -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments = seeded ? ["-UITestMock", "-UITestSeed"] : ["-UITestMock"]
        app.launch()
        XCTAssertTrue(app.buttons["add-task"].waitForExistence(timeout: 10))
        return app
    }

    /// Counts expose their number as the accessibility value. The overdue line exposes its text as the label.
    private func count(_ app: XCUIApplication, _ id: String) -> String {
        let element = app.descendants(matching: .any).matching(identifier: id).firstMatch
        return id == "summary-overdue" ? element.label : (element.value as? String ?? "")
    }

    @MainActor
    func testCreateTask() {
        let app = launch(seeded: false)
        XCTAssertTrue(app.staticTexts["empty-state"].waitForExistence(timeout: 5))
        app.buttons["add-task"].tap()
        let title = app.textFields["field-title"]
        XCTAssertTrue(title.waitForExistence(timeout: 5))
        title.typeText("Pay rent")
        app.buttons["form-save"].tap()
        XCTAssertTrue(app.buttons["task-title-mock-1"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["toast"].waitForExistence(timeout: 5))
        XCTAssertEqual(app.staticTexts["toast"].label, "Task created")
        XCTAssertFalse(app.textFields["field-title"].exists)
    }

    @MainActor
    func testTitleTooLong() {
        let app = launch(seeded: false)
        app.buttons["add-task"].tap()
        let title = app.textFields["field-title"]
        XCTAssertTrue(title.waitForExistence(timeout: 5))
        title.typeText(String(repeating: "a", count: 101))
        XCTAssertTrue(app.staticTexts["error-title"].waitForExistence(timeout: 5))
        XCTAssertEqual(app.staticTexts["error-title"].label, "Title must be 100 characters or fewer")
        XCTAssertFalse(app.buttons["form-save"].isEnabled)
    }

    @MainActor
    func testFilterAndEmptyState() {
        let app = launch(seeded: true)
        XCTAssertTrue(app.buttons["task-title-seed-1"].waitForExistence(timeout: 5))
        app.buttons["chip-status-TODO"].tap()
        app.buttons["chip-priority-HIGH"].tap()
        XCTAssertTrue(app.buttons["task-title-seed-1"].waitForExistence(timeout: 5))
        XCTAssertTrue(waitForDisappearance(app.buttons["task-title-seed-2"]))
        XCTAssertTrue(waitForDisappearance(app.buttons["task-title-seed-3"]))
        app.buttons["chip-priority-LOW"].tap()
        XCTAssertTrue(app.staticTexts["No tasks match these filters."].waitForExistence(timeout: 5))
        XCTAssertTrue(app.buttons["Clear filters"].exists)
    }

    @MainActor
    func testStatusChange() {
        let app = launch(seeded: true)
        XCTAssertTrue(app.buttons["status-menu-seed-1"].waitForExistence(timeout: 5))
        XCTAssertEqual(count(app, "summary-done"), "1")
        app.buttons["status-menu-seed-1"].tap()
        let done = app.buttons["status-option-DONE"]
        XCTAssertTrue(done.waitForExistence(timeout: 5))
        done.tap()
        let updated = NSPredicate(format: "value == %@", "2")
        expectation(for: updated, evaluatedWith: app.descendants(matching: .any).matching(identifier: "summary-done").firstMatch)
        waitForExpectations(timeout: 5)
        XCTAssertTrue(app.buttons["status-menu-seed-1"].label.contains("Done"))
    }

    @MainActor
    func testDeleteCancelAndConfirm() {
        let app = launch(seeded: true)
        XCTAssertTrue(app.buttons["delete-seed-1"].waitForExistence(timeout: 5))
        XCTAssertEqual(count(app, "summary-total"), "3")
        app.buttons["delete-seed-1"].tap()
        XCTAssertTrue(app.alerts["Delete task?"].waitForExistence(timeout: 5))
        app.alerts.buttons["Cancel"].tap()
        XCTAssertTrue(waitForDisappearance(app.alerts["Delete task?"]))
        XCTAssertTrue(app.buttons["task-title-seed-1"].exists)
        app.buttons["delete-seed-1"].tap()
        XCTAssertTrue(app.alerts["Delete task?"].waitForExistence(timeout: 5))
        app.alerts.buttons["Delete"].tap()
        XCTAssertTrue(waitForDisappearance(app.buttons["task-title-seed-1"]))
        let updated = NSPredicate(format: "value == %@", "2")
        expectation(for: updated, evaluatedWith: app.descendants(matching: .any).matching(identifier: "summary-total").firstMatch)
        waitForExpectations(timeout: 5)
    }

    @MainActor
    func testOverdueRow() {
        let app = launch(seeded: true)
        XCTAssertTrue(app.staticTexts["Overdue"].waitForExistence(timeout: 5))
        XCTAssertEqual(count(app, "summary-overdue"), "Overdue: 1")
    }

    @MainActor
    func testEmptySummaryShowsZeros() {
        let app = launch(seeded: false)
        XCTAssertTrue(app.descendants(matching: .any).matching(identifier: "summary-total").firstMatch.waitForExistence(timeout: 5))
        for id in ["summary-total", "summary-todo", "summary-in-progress", "summary-done"] {
            XCTAssertEqual(count(app, id), "0", id)
        }
        XCTAssertEqual(count(app, "summary-overdue"), "Overdue: 0")
    }

    private func waitForDisappearance(_ element: XCUIElement, timeout: TimeInterval = 5) -> Bool {
        let gone = NSPredicate(format: "exists == false")
        return XCTWaiter().wait(for: [XCTNSPredicateExpectation(predicate: gone, object: element)],
                                timeout: timeout) == .completed
    }
}
