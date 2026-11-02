import Foundation

/// In-memory API for UI tests and unit tests. It mirrors the server rules for the cases the app uses.
actor MockTaskAPI: TaskAPI {
    static let today = "2026-11-02"

    private var tasks: [TaskItem]
    private var nextId = 1
    private var queuedFailure: APIError?

    init(seed: [TaskItem] = []) {
        tasks = seed
        nextId = seed.count + 1
    }

    static var seeded: [TaskItem] {
        [
            TaskItem(id: "seed-1", title: "Write report", description: nil, priority: .high,
                     status: .todo, dueDate: "2026-11-10", overdue: false),
            TaskItem(id: "seed-2", title: "Buy milk", description: nil, priority: .low,
                     status: .done, dueDate: "2026-11-05", overdue: false),
            TaskItem(id: "seed-3", title: "Old invoice", description: nil, priority: .medium,
                     status: .inProgress, dueDate: "2026-10-30", overdue: true),
        ]
    }

    /// The next call throws this error, then the mock works again.
    func failNext(_ error: APIError) {
        queuedFailure = error
    }

    func list(filters: TaskFilters) throws -> [TaskItem] {
        try consumeFailure()
        return tasks
            .map(withOverdue)
            .filter { filters.status == nil || $0.status == filters.status }
            .filter { filters.priority == nil || $0.priority == filters.priority }
            .sorted { $0.dueDate < $1.dueDate }
    }

    func create(_ draft: TaskDraft) throws -> TaskItem {
        try consumeFailure()
        try validate(draft, checkDate: true)
        let task = TaskItem(
            id: "mock-\(nextId)", title: draft.title.trimmingCharacters(in: .whitespaces),
            description: draft.description.isEmpty ? nil : draft.description,
            priority: draft.priority, status: .todo, dueDate: draft.dueDate,
            overdue: false
        )
        nextId += 1
        tasks.append(task)
        return task
    }

    func update(id: String, draft: TaskDraft) throws -> TaskItem {
        try consumeFailure()
        let current = try find(id)
        try validate(draft, checkDate: draft.dueDate != current.dueDate)
        return replace(TaskItem(
            id: id, title: draft.title.trimmingCharacters(in: .whitespaces),
            description: draft.description.isEmpty ? nil : draft.description,
            priority: draft.priority, status: current.status, dueDate: draft.dueDate,
            overdue: false
        ))
    }

    func setStatus(id: String, status: Status) throws -> TaskItem {
        try consumeFailure()
        let current = try find(id)
        return replace(TaskItem(
            id: id, title: current.title, description: current.description,
            priority: current.priority, status: status, dueDate: current.dueDate,
            overdue: false
        ))
    }

    func delete(id: String) throws {
        try consumeFailure()
        _ = try find(id)
        tasks.removeAll { $0.id == id }
    }

    func summary() throws -> Summary {
        try consumeFailure()
        let all = tasks.map(withOverdue)
        return Summary(
            total: all.count,
            todo: all.filter { $0.status == .todo }.count,
            inProgress: all.filter { $0.status == .inProgress }.count,
            done: all.filter { $0.status == .done }.count,
            overdue: all.filter(\.overdue).count
        )
    }

    private func consumeFailure() throws {
        guard let failure = queuedFailure else { return }
        queuedFailure = nil
        throw failure
    }

    private func find(_ id: String) throws -> TaskItem {
        guard let task = tasks.first(where: { $0.id == id }) else { throw APIError.notFound }
        return task
    }

    private func replace(_ task: TaskItem) -> TaskItem {
        let updated = withOverdue(task)
        if let index = tasks.firstIndex(where: { $0.id == task.id }) { tasks[index] = updated }
        return updated
    }

    private func withOverdue(_ task: TaskItem) -> TaskItem {
        TaskItem(
            id: task.id, title: task.title, description: task.description,
            priority: task.priority, status: task.status, dueDate: task.dueDate,
            overdue: task.dueDate < Self.today && task.status != .done
        )
    }

    private func validate(_ draft: TaskDraft, checkDate: Bool) throws {
        var fields: [String: String] = [:]
        let title = draft.title.trimmingCharacters(in: .whitespaces)
        if title.isEmpty { fields["title"] = "Title is required" }
        if title.count > 100 { fields["title"] = "Title must be 100 characters or fewer" }
        if checkDate && draft.dueDate < Self.today { fields["dueDate"] = "Due date cannot be in the past" }
        if !fields.isEmpty { throw APIError.validation(fields: fields, message: "Invalid request") }
    }
}
