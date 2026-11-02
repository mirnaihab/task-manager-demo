import Foundation
import Observation

enum Phase: Equatable { case loading, content, error }

/// One-shot toast texts. The view maps each value to its text.
enum UiMessage: Equatable {
    case taskCreated, taskUpdated, taskDeleted, taskGone, networkError, serverError

    var text: String {
        switch self {
        case .taskCreated: "Task created"
        case .taskUpdated: "Task updated"
        case .taskDeleted: "Task deleted"
        case .taskGone: "Task no longer exists"
        case .networkError: "Cannot reach the server."
        case .serverError: "Something went wrong. Try again."
        }
    }
}

enum TitleError: Equatable {
    case required, tooLong

    var text: String {
        switch self {
        case .required: "Title is required"
        case .tooLong: "Title must be 100 characters or fewer"
        }
    }
}

struct FormState: Equatable {
    var editingId: String?
    var draft: TaskDraft
    var titleError: TitleError?
    var serverErrors: [String: String] = [:]
    var isSaving = false
}

@Observable
@MainActor
final class TaskListViewModel {
    private(set) var tasks: [TaskItem] = []
    private(set) var summary = Summary()
    private(set) var filters = TaskFilters()
    private(set) var phase: Phase = .loading
    private(set) var isRefreshing = false
    var form: FormState?
    var pendingDelete: TaskItem?
    var message: UiMessage?

    private let api: TaskAPI
    private let today: () -> String
    private var loadTask: Task<Void, Never>?

    init(api: TaskAPI, today: @escaping () -> String) {
        self.api = api
        self.today = today
    }

    func start() {
        reload()
    }

    func retry() {
        phase = .loading
        reload()
    }

    /// A failed refresh keeps the data on screen and shows a message.
    func refresh() async {
        launchLoad(showErrorState: false)
        await loadTask?.value
    }

    func setStatusFilter(_ status: Status?) {
        filters.status = status
        reload()
    }

    func setPriorityFilter(_ priority: Priority?) {
        filters.priority = priority
        reload()
    }

    func clearFilters() {
        filters = TaskFilters()
        reload()
    }

    func openCreate() {
        form = FormState(editingId: nil, draft: TaskDraft(dueDate: today()))
    }

    func openEdit(_ task: TaskItem) {
        let draft = TaskDraft(title: task.title, description: task.description ?? "",
                              priority: task.priority, dueDate: task.dueDate)
        form = FormState(editingId: task.id, draft: draft)
    }

    func closeForm() {
        form = nil
    }

    func updateDraft(_ draft: TaskDraft) {
        guard var current = form else { return }
        current.draft = draft
        current.titleError = draft.title.trimmingCharacters(in: .whitespacesAndNewlines).count > Theme.titleMax
            ? .tooLong : nil
        current.serverErrors = [:]
        form = current
    }

    func save() async {
        guard var current = form, !current.isSaving else { return }
        if let error = validateTitle(current.draft.title) {
            current.titleError = error
            form = current
            return
        }
        current.isSaving = true
        current.serverErrors = [:]
        form = current
        let snapshot = current
        do {
            if let id = snapshot.editingId {
                _ = try await api.update(id: id, draft: snapshot.draft)
            } else {
                _ = try await api.create(snapshot.draft)
            }
            form = nil
            message = snapshot.editingId == nil ? .taskCreated : .taskUpdated
            await refresh()
        } catch APIError.validation(let fields, _) {
            form?.serverErrors = fields
            form?.isSaving = false
        } catch APIError.notFound {
            form = nil
            message = .taskGone
            await refresh()
        } catch {
            form?.isSaving = false
            message = Self.message(for: error)
        }
    }

    func changeStatus(_ task: TaskItem, to status: Status) async {
        guard task.status != status else { return }
        replace(TaskItem(id: task.id, title: task.title, description: task.description,
                         priority: task.priority, status: status, dueDate: task.dueDate,
                         overdue: task.overdue))
        do {
            _ = try await api.setStatus(id: task.id, status: status)
            await refresh()
        } catch {
            replace(task)
            message = Self.message(for: error)
        }
    }

    func askDelete(_ task: TaskItem) {
        pendingDelete = task
    }

    func cancelDelete() {
        pendingDelete = nil
    }

    func confirmDelete(_ task: TaskItem) async {
        pendingDelete = nil
        do {
            try await api.delete(id: task.id)
            tasks.removeAll { $0.id == task.id }
            message = .taskDeleted
        } catch APIError.notFound {
            message = .taskGone
        } catch {
            message = Self.message(for: error)
            return
        }
        await refresh()
    }

    func consumeMessage() {
        message = nil
    }

    private func reload() {
        launchLoad(showErrorState: true)
    }

    // The newest load wins (EDGE-P3-001).
    private func launchLoad(showErrorState: Bool) {
        loadTask?.cancel()
        let requested = filters
        loadTask = Task {
            isRefreshing = true
            do {
                async let fetchedTasks = api.list(filters: requested)
                async let fetchedSummary = api.summary()
                let (newTasks, newSummary) = try await (fetchedTasks, fetchedSummary)
                if Task.isCancelled { return }
                tasks = newTasks
                summary = newSummary
                phase = .content
                isRefreshing = false
            } catch {
                // A cancelled load is replaced by a newer one. It must not show an error.
                if Task.isCancelled || Self.isCancellation(error) { return }
                isRefreshing = false
                if showErrorState {
                    phase = .error
                } else {
                    message = Self.message(for: error)
                }
            }
        }
    }

    private func replace(_ task: TaskItem) {
        tasks = tasks.map { $0.id == task.id ? task : $0 }
    }

    private func validateTitle(_ title: String) -> TitleError? {
        let trimmed = title.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty { return .required }
        if trimmed.count > Theme.titleMax { return .tooLong }
        return nil
    }

    private static func isCancellation(_ error: Error) -> Bool {
        error is CancellationError || (error as? URLError)?.code == .cancelled
    }

    private static func message(for error: Error) -> UiMessage {
        (error as? APIError) == .network ? .networkError : .serverError
    }
}
