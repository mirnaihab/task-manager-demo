import SwiftUI

struct TaskListView: View {
    @Bindable var viewModel: TaskListViewModel

    var body: some View {
        NavigationStack {
            content
                .background(Theme.surface)
                .navigationTitle("Tasks")
                .toolbarTitleDisplayMode(.inline)
        }
        .overlay(alignment: .bottomTrailing) { addButton }
        .overlay(alignment: .bottom) { toast }
        .fullScreenCover(isPresented: formBinding) {
            TaskFormView(viewModel: viewModel)
        }
        .alert("Delete task?", isPresented: deleteBinding, presenting: viewModel.pendingDelete) { task in
            Button("Cancel", role: .cancel) { viewModel.cancelDelete() }
            Button("Delete", role: .destructive) { Task { await viewModel.confirmDelete(task) } }
        } message: { task in
            Text("\(task.title)\nThis cannot be undone.")
        }
        .task { viewModel.start() }
        .task(id: viewModel.message) { await showToast() }
    }

    @ViewBuilder
    private var content: some View {
        switch viewModel.phase {
        case .loading:
            ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
        case .error:
            errorState
        case .content:
            list
        }
    }

    private var list: some View {
        List {
            Section {
                VStack(alignment: .leading, spacing: Theme.Spacing.md) {
                    SummaryPanelView(summary: viewModel.summary)
                    FilterChipsView(filters: viewModel.filters,
                                    onStatus: viewModel.setStatusFilter,
                                    onPriority: viewModel.setPriorityFilter)
                }
                .padding(Theme.Spacing.md)
                .listRowInsets(EdgeInsets())
                .listRowBackground(Theme.surface)
                .listRowSeparator(.hidden)
            }
            if viewModel.tasks.isEmpty {
                emptyState
                    .listRowBackground(Theme.surface)
                    .listRowSeparator(.hidden)
            } else {
                ForEach(viewModel.tasks) { task in
                    TaskRowView(
                        task: task,
                        onEdit: { viewModel.openEdit(task) },
                        onStatus: { status in Task { await viewModel.changeStatus(task, to: status) } },
                        onDelete: { viewModel.askDelete(task) }
                    )
                    .listRowInsets(EdgeInsets())
                    .listRowSeparatorTint(Theme.outline)
                    .listRowBackground(Theme.surface)
                }
            }
            Color.clear.frame(height: 80)
                .listRowBackground(Theme.surface)
                .listRowSeparator(.hidden)
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
        .refreshable { await viewModel.refresh() }
        .frame(maxWidth: Theme.maxContentWidth)
        .frame(maxWidth: .infinity)
    }

    private var emptyState: some View {
        VStack(spacing: Theme.Spacing.md) {
            Text(viewModel.filters.isActive ? "No tasks match these filters." : "No tasks yet. Tap + to add one.")
                .font(.system(size: 15))
                .foregroundStyle(Theme.onSurfaceVariant)
                .multilineTextAlignment(.center)
                .accessibilityIdentifier("empty-state")
            if viewModel.filters.isActive {
                Button("Clear filters") { viewModel.clearFilters() }
                    .foregroundStyle(Theme.primary)
                    .frame(minHeight: Theme.minTouchTarget)
                    .accessibilityIdentifier("clear-filters")
            }
        }
        .frame(maxWidth: .infinity)
        .padding(Theme.Spacing.lg)
    }

    private var errorState: some View {
        VStack(spacing: Theme.Spacing.md) {
            Text("Cannot reach the server.")
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(Theme.onSurface)
            Button("Try again") { viewModel.retry() }
                .buttonStyle(.borderedProminent)
                .tint(Theme.primary)
                .accessibilityIdentifier("try-again")
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var addButton: some View {
        Button { viewModel.openCreate() } label: {
            Image(systemName: "plus")
                .font(.title2.weight(.semibold))
                .foregroundStyle(Theme.onPrimary)
                .frame(width: 56, height: 56)
                .background(Theme.primary, in: Circle())
                .shadow(radius: 4, y: 2)
        }
        .padding(Theme.Spacing.md)
        .accessibilityLabel("Add task")
        .accessibilityIdentifier("add-task")
    }

    @ViewBuilder
    private var toast: some View {
        if let message = viewModel.message {
            ToastView(text: message.text)
                .padding(.bottom, Theme.Spacing.xl + Theme.Spacing.lg)
                .transition(.opacity)
                .allowsHitTesting(false)
        }
    }

    private var formBinding: Binding<Bool> {
        Binding(get: { viewModel.form != nil }, set: { if !$0 { viewModel.closeForm() } })
    }

    private var deleteBinding: Binding<Bool> {
        Binding(get: { viewModel.pendingDelete != nil }, set: { if !$0 { viewModel.cancelDelete() } })
    }

    // The toast shows for 3 seconds. A new message restarts the timer.
    private func showToast() async {
        guard viewModel.message != nil else { return }
        try? await Task.sleep(for: .seconds(3))
        if !Task.isCancelled { viewModel.consumeMessage() }
    }
}
