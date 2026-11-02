import Foundation

nonisolated enum Priority: String, Codable, CaseIterable, Sendable {
    case low = "LOW"
    case medium = "MEDIUM"
    case high = "HIGH"

    var label: String {
        switch self {
        case .low: "Low"
        case .medium: "Medium"
        case .high: "High"
        }
    }
}

nonisolated enum Status: String, Codable, CaseIterable, Sendable {
    case todo = "TODO"
    case inProgress = "IN_PROGRESS"
    case done = "DONE"

    var label: String {
        switch self {
        case .todo: "To Do"
        case .inProgress: "In Progress"
        case .done: "Done"
        }
    }
}

/// `dueDate` uses the API format yyyy-MM-dd, so no time zone conversion happens in data.
nonisolated struct TaskItem: Codable, Identifiable, Equatable, Sendable {
    let id: String
    let title: String
    let description: String?
    let priority: Priority
    let status: Status
    let dueDate: String
    let overdue: Bool
}

nonisolated struct Summary: Codable, Equatable, Sendable {
    var total = 0
    var todo = 0
    var inProgress = 0
    var done = 0
    var overdue = 0
}

nonisolated struct TaskFilters: Equatable, Sendable {
    var status: Status?
    var priority: Priority?

    var isActive: Bool { status != nil || priority != nil }
}

nonisolated struct TaskDraft: Equatable, Sendable {
    var title = ""
    var description = ""
    var priority: Priority = .medium
    var dueDate: String
}
