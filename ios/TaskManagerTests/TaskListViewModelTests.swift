import XCTest
@testable import TaskManager

@MainActor
final class TaskListViewModelTests: XCTestCase {

    private func makeViewModel(seed: [TaskItem] = MockTaskAPI.seeded) -> (TaskListViewModel, MockTaskAPI) {
        let api = MockTaskAPI(seed: seed)
        let viewModel = TaskListViewModel(api: api, today: { MockTaskAPI.today })
        return (viewModel, api)
    }

    private func loaded(seed: [TaskItem] = MockTaskAPI.seeded) async -> (TaskListViewModel, MockTaskAPI) {
        let (viewModel, api) = makeViewModel(seed: seed)
        viewModel.start()
        await viewModel.refresh()
        return (viewModel, api)
    }

    func testREQ301_loadsTasksAndSummary() async {
        let (viewModel, _) = await loaded()
        XCTAssertEqual(viewModel.phase, .content)
        XCTAssertEqual(viewModel.tasks.count, 3)
        XCTAssertEqual(viewModel.summary.total, 3)
        XCTAssertEqual(viewModel.summary.overdue, 1)
    }

    func testREQ303_filtersCombine() async {
        let (viewModel, _) = await loaded()
        viewModel.setStatusFilter(.todo)
        viewModel.setPriorityFilter(.high)
        await viewModel.refresh()
        XCTAssertEqual(viewModel.tasks.map(\.id), ["seed-1"])
        viewModel.setPriorityFilter(.low)
        await viewModel.refresh()
        XCTAssertTrue(viewModel.tasks.isEmpty)
        viewModel.clearFilters()
        await viewModel.refresh()
        XCTAssertEqual(viewModel.tasks.count, 3)
    }

    func testREQ305_openCreateUsesDefaults() async {
        let (viewModel, _) = await loaded()
        viewModel.openCreate()
        XCTAssertEqual(viewModel.form?.draft.title, "")
        XCTAssertEqual(viewModel.form?.draft.priority, .medium)
        XCTAssertEqual(viewModel.form?.draft.dueDate, "2026-11-02")
    }

    func testREQ306_saveCreatesClosesFormAndRefreshes() async {
        let (viewModel, _) = await loaded(seed: [])
        viewModel.openCreate()
        var draft = viewModel.form!.draft
        draft.title = "Pay rent"
        viewModel.updateDraft(draft)
        await viewModel.save()
        XCTAssertNil(viewModel.form)
        XCTAssertEqual(viewModel.message, .taskCreated)
        XCTAssertEqual(viewModel.tasks.map(\.title), ["Pay rent"])
        XCTAssertEqual(viewModel.summary.todo, 1)
    }

    func testREQ306_saveEditShowsUpdated() async {
        let (viewModel, _) = await loaded()
        viewModel.openEdit(viewModel.tasks[0])
        var draft = viewModel.form!.draft
        draft.title = "Renamed"
        viewModel.updateDraft(draft)
        await viewModel.save()
        XCTAssertEqual(viewModel.message, .taskUpdated)
        XCTAssertTrue(viewModel.tasks.contains { $0.title == "Renamed" })
    }

    func testREQ307_validationFieldsStayOnForm() async {
        let (viewModel, api) = await loaded()
        viewModel.openCreate()
        var draft = viewModel.form!.draft
        draft.title = "Ok"
        viewModel.updateDraft(draft)
        await api.failNext(.validation(fields: ["dueDate": "Due date cannot be in the past"], message: "Invalid"))
        await viewModel.save()
        XCTAssertNotNil(viewModel.form)
        XCTAssertEqual(viewModel.form?.serverErrors["dueDate"], "Due date cannot be in the past")
        XCTAssertEqual(viewModel.form?.isSaving, false)
    }

    func testREQ308_emptyTitleBlocksSave() async {
        let (viewModel, _) = await loaded(seed: [])
        viewModel.openCreate()
        await viewModel.save()
        XCTAssertEqual(viewModel.form?.titleError, .required)
        XCTAssertNil(viewModel.message)
    }

    func testREQ308_titleOver100CharsIsTooLong() async {
        let (viewModel, _) = await loaded(seed: [])
        viewModel.openCreate()
        var draft = viewModel.form!.draft
        draft.title = String(repeating: "a", count: 101)
        viewModel.updateDraft(draft)
        XCTAssertEqual(viewModel.form?.titleError, .tooLong)
        draft.title = String(repeating: "a", count: 100)
        viewModel.updateDraft(draft)
        XCTAssertNil(viewModel.form?.titleError)
    }

    func testEDGE003_doubleSaveSendsOneRequest() async {
        let (viewModel, _) = await loaded(seed: [])
        viewModel.openCreate()
        var draft = viewModel.form!.draft
        draft.title = "Once"
        viewModel.updateDraft(draft)
        async let first: Void = viewModel.save()
        async let second: Void = viewModel.save()
        _ = await (first, second)
        XCTAssertEqual(viewModel.tasks.count, 1)
    }

    func testEDGE004_notFoundOnEditClosesFormWithToast() async {
        let (viewModel, api) = await loaded()
        viewModel.openEdit(viewModel.tasks[0])
        await api.failNext(.notFound)
        await viewModel.save()
        XCTAssertNil(viewModel.form)
        XCTAssertEqual(viewModel.message, .taskGone)
    }

    func testEDGE010_networkErrorOnSaveKeepsForm() async {
        let (viewModel, api) = await loaded(seed: [])
        viewModel.openCreate()
        var draft = viewModel.form!.draft
        draft.title = "Keep me"
        viewModel.updateDraft(draft)
        await api.failNext(.network)
        await viewModel.save()
        XCTAssertEqual(viewModel.form?.draft.title, "Keep me")
        XCTAssertEqual(viewModel.form?.isSaving, false)
        XCTAssertEqual(viewModel.message, .networkError)
    }

    func testREQ310_statusChangeUpdatesRowAndSummary() async {
        let (viewModel, _) = await loaded()
        let task = viewModel.tasks.first { $0.id == "seed-1" }!
        await viewModel.changeStatus(task, to: .done)
        XCTAssertEqual(viewModel.tasks.first { $0.id == "seed-1" }?.status, .done)
        XCTAssertEqual(viewModel.summary.done, 2)
    }

    func testREQ310_failedStatusChangeRevertsAndShowsToast() async {
        let (viewModel, api) = await loaded()
        let task = viewModel.tasks[0]
        await api.failNext(.server)
        await viewModel.changeStatus(task, to: .done)
        XCTAssertEqual(viewModel.tasks.first { $0.id == task.id }?.status, task.status)
        XCTAssertEqual(viewModel.message, .serverError)
    }

    func testREQ311_cancelDeleteChangesNothing() async {
        let (viewModel, _) = await loaded()
        viewModel.askDelete(viewModel.tasks[0])
        XCTAssertNotNil(viewModel.pendingDelete)
        viewModel.cancelDelete()
        XCTAssertNil(viewModel.pendingDelete)
        XCTAssertEqual(viewModel.tasks.count, 3)
    }

    func testREQ311_confirmDeleteRemovesRowAndRefreshesSummary() async {
        let (viewModel, _) = await loaded()
        let task = viewModel.tasks[0]
        viewModel.askDelete(task)
        await viewModel.confirmDelete(task)
        XCTAssertFalse(viewModel.tasks.contains { $0.id == task.id })
        XCTAssertEqual(viewModel.summary.total, 2)
        XCTAssertEqual(viewModel.message, .taskDeleted)
    }

    func testREQ312_emptySummaryHasZeros() async {
        let (viewModel, _) = await loaded(seed: [])
        XCTAssertEqual(viewModel.summary, Summary())
    }

    func testREQ313_overdueComesFromServer() async {
        let (viewModel, _) = await loaded()
        XCTAssertEqual(viewModel.tasks.first { $0.id == "seed-3" }?.overdue, true)
        XCTAssertEqual(viewModel.tasks.first { $0.id == "seed-1" }?.overdue, false)
    }

    func testREQ314_failedLoadMapsToErrorPhase() async {
        let api = FailingAPI()
        let viewModel = TaskListViewModel(api: api, today: { MockTaskAPI.today })
        viewModel.start()
        await waitUntil { viewModel.phase == .error }
        XCTAssertEqual(viewModel.phase, .error)
    }

    func testEDGE001_newestFilterWins() async {
        let api = SlowFirstAPI()
        let viewModel = TaskListViewModel(api: api, today: { MockTaskAPI.today })
        viewModel.setStatusFilter(.todo)
        viewModel.setStatusFilter(.done)
        await waitUntil { viewModel.phase == .content }
        try? await Task.sleep(for: .milliseconds(300))
        XCTAssertEqual(viewModel.filters.status, .done)
        XCTAssertEqual(viewModel.tasks.map(\.title), ["done"])
    }

    private func waitUntil(_ condition: @MainActor () -> Bool) async {
        for _ in 0..<100 where !condition() {
            try? await Task.sleep(for: .milliseconds(20))
        }
    }
}

private struct FailingAPI: TaskAPI {
    func list(filters: TaskFilters) async throws -> [TaskItem] { throw APIError.network }
    func create(_ draft: TaskDraft) async throws -> TaskItem { throw APIError.network }
    func update(id: String, draft: TaskDraft) async throws -> TaskItem { throw APIError.network }
    func setStatus(id: String, status: Status) async throws -> TaskItem { throw APIError.network }
    func delete(id: String) async throws { throw APIError.network }
    func summary() async throws -> Summary { throw APIError.network }
}

/// The TODO list call is slow and finishes after the DONE call. The DONE result must stay.
private struct SlowFirstAPI: TaskAPI {
    private func item(_ title: String, _ status: Status) -> TaskItem {
        TaskItem(id: title, title: title, description: nil, priority: .low, status: status,
                 dueDate: "2026-12-01", overdue: false)
    }

    func list(filters: TaskFilters) async throws -> [TaskItem] {
        if filters.status == .todo {
            try await Task.sleep(for: .milliseconds(150))
            return [item("todo", .todo)]
        }
        return [item("done", .done)]
    }

    func create(_ draft: TaskDraft) async throws -> TaskItem { item("x", .todo) }
    func update(id: String, draft: TaskDraft) async throws -> TaskItem { item("x", .todo) }
    func setStatus(id: String, status: Status) async throws -> TaskItem { item("x", status) }
    func delete(id: String) async throws {}
    func summary() async throws -> Summary { Summary() }
}
