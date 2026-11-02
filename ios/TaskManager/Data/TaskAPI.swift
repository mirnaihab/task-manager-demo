import Foundation

nonisolated protocol TaskAPI: Sendable {
    func list(filters: TaskFilters) async throws -> [TaskItem]
    func create(_ draft: TaskDraft) async throws -> TaskItem
    func update(id: String, draft: TaskDraft) async throws -> TaskItem
    func setStatus(id: String, status: Status) async throws -> TaskItem
    func delete(id: String) async throws
    func summary() async throws -> Summary
}

nonisolated enum APIError: Error, Equatable {
    case validation(fields: [String: String], message: String)
    case notFound
    case server
    case network
}
