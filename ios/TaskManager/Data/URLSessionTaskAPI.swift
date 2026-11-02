import Foundation

/// Calls the REST API. Only the status code and the validation fields leave this type.
/// It never logs or reports request or response bodies.
nonisolated struct URLSessionTaskAPI: TaskAPI {
    private let baseURL: URL
    private let session: URLSession
    private let decoder = JSONDecoder()
    private let encoder = JSONEncoder()

    init(baseURL: URL, session: URLSession = URLSessionTaskAPI.makeSession()) {
        self.baseURL = baseURL
        self.session = session
    }

    static func makeSession() -> URLSession {
        let configuration = URLSessionConfiguration.ephemeral
        configuration.timeoutIntervalForRequest = 15
        return URLSession(configuration: configuration)
    }

    func list(filters: TaskFilters) async throws -> [TaskItem] {
        var query: [URLQueryItem] = []
        if let status = filters.status { query.append(URLQueryItem(name: "status", value: status.rawValue)) }
        if let priority = filters.priority { query.append(URLQueryItem(name: "priority", value: priority.rawValue)) }
        return try await send("GET", "api/tasks", query: query)
    }

    func create(_ draft: TaskDraft) async throws -> TaskItem {
        let body = CreateBody(title: draft.title, description: draft.description,
                              priority: draft.priority, dueDate: draft.dueDate)
        return try await send("POST", "api/tasks", body: body)
    }

    func update(id: String, draft: TaskDraft) async throws -> TaskItem {
        let body = UpdateBody(title: draft.title, description: draft.description,
                              priority: draft.priority, dueDate: draft.dueDate)
        return try await send("PATCH", "api/tasks/\(id)", body: body)
    }

    func setStatus(id: String, status: Status) async throws -> TaskItem {
        try await send("PATCH", "api/tasks/\(id)", body: UpdateBody(status: status))
    }

    func delete(id: String) async throws {
        _ = try await execute("DELETE", "api/tasks/\(id)", query: [], body: nil as CreateBody?)
    }

    func summary() async throws -> Summary {
        try await send("GET", "api/summary")
    }

    private func send<Response: Decodable, Body: Encodable>(
        _ method: String, _ path: String, query: [URLQueryItem] = [], body: Body?
    ) async throws -> Response {
        let data = try await execute(method, path, query: query, body: body)
        do {
            return try decoder.decode(Response.self, from: data)
        } catch {
            throw APIError.server
        }
    }

    private func send<Response: Decodable>(
        _ method: String, _ path: String, query: [URLQueryItem] = []
    ) async throws -> Response {
        try await send(method, path, query: query, body: nil as CreateBody?)
    }

    private func execute<Body: Encodable>(
        _ method: String, _ path: String, query: [URLQueryItem], body: Body?
    ) async throws -> Data {
        var components = URLComponents(url: baseURL.appendingPathComponent(path), resolvingAgainstBaseURL: false)
        if !query.isEmpty { components?.queryItems = query }
        guard let url = components?.url else { throw APIError.server }
        var request = URLRequest(url: url)
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        if let body {
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            request.httpBody = try encoder.encode(body)
        }
        let data: Data
        let response: URLResponse
        do {
            (data, response) = try await session.data(for: request)
        } catch let error as URLError where error.code != .cancelled {
            throw APIError.network
        }
        guard let http = response as? HTTPURLResponse else { throw APIError.server }
        switch http.statusCode {
        case 200, 201, 204: return data
        case 400: throw validationError(from: data)
        case 404: throw APIError.notFound
        default: throw APIError.server
        }
    }

    private func validationError(from data: Data) -> APIError {
        guard let envelope = try? decoder.decode(ErrorEnvelope.self, from: data),
              envelope.error.code == "VALIDATION_ERROR" else { return .server }
        return .validation(fields: envelope.error.fields ?? [:], message: envelope.error.message)
    }
}

private nonisolated struct CreateBody: Encodable {
    let title: String
    let description: String
    let priority: Priority
    let dueDate: String
}

/// Unset fields stay out of the JSON body. An empty description clears it.
private nonisolated struct UpdateBody: Encodable {
    var title: String?
    var description: String?
    var priority: Priority?
    var status: Status?
    var dueDate: String?
}

private nonisolated struct ErrorEnvelope: Decodable {
    struct Body: Decodable {
        let code: String
        let message: String
        let fields: [String: String]?
    }
    let error: Body
}
