import SwiftUI

@main
struct TaskManagerApp: App {
    @State private var viewModel: TaskListViewModel?

    init() {
        let api = AppEnvironment.makeAPI()
        assert(api != nil, "API_BASE_URL is missing or invalid")
        _viewModel = State(initialValue: api.map {
            TaskListViewModel(api: $0, today: { AppEnvironment.todayString() })
        })
    }

    var body: some Scene {
        WindowGroup {
            if let viewModel {
                TaskListView(viewModel: viewModel)
            } else {
                Text("The app is not configured.")
                    .padding(Theme.Spacing.md)
            }
        }
    }
}
