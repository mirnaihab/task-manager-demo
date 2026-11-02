import SwiftUI

struct TaskFormView: View {
    @Bindable var viewModel: TaskListViewModel
    @FocusState private var titleFocused: Bool

    var body: some View {
        NavigationStack {
            if let form = viewModel.form {
                ScrollView {
                    VStack(alignment: .leading, spacing: Theme.Spacing.md) {
                        titleField(form)
                        descriptionField(form)
                        priorityField(form)
                        dueDateField(form)
                    }
                    .padding(Theme.Spacing.md)
                    .frame(maxWidth: Theme.maxContentWidth)
                    .frame(maxWidth: .infinity)
                }
                .background(Theme.surface)
                .scrollDismissesKeyboard(.interactively)
                .navigationTitle(form.editingId == nil ? "New task" : "Edit task")
                .toolbarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button { viewModel.closeForm() } label: { Image(systemName: "xmark") }
                            .accessibilityLabel("Close")
                            .accessibilityIdentifier("form-close")
                    }
                }
                .safeAreaInset(edge: .bottom) { actions(form) }
                .onAppear { titleFocused = true }
            }
        }
    }

    private func draftBinding<Value>(_ keyPath: WritableKeyPath<TaskDraft, Value>, _ form: FormState) -> Binding<Value> {
        Binding(
            get: { viewModel.form?.draft[keyPath: keyPath] ?? form.draft[keyPath: keyPath] },
            set: { value in
                var draft = viewModel.form?.draft ?? form.draft
                draft[keyPath: keyPath] = value
                viewModel.updateDraft(draft)
            }
        )
    }

    private func titleField(_ form: FormState) -> some View {
        let error = form.titleError?.text ?? form.serverErrors["title"]
        return VStack(alignment: .leading, spacing: Theme.Spacing.xs) {
            Text("Title").font(.caption.weight(.medium)).foregroundStyle(Theme.onSurfaceVariant)
            TextField("Title", text: draftBinding(\.title, form))
                .focused($titleFocused)
                .font(.system(size: 15))
                .inputStyle(hasError: error != nil)
                .accessibilityIdentifier("field-title")
            HStack(alignment: .top) {
                if let error {
                    Text(error).foregroundStyle(Theme.error).accessibilityIdentifier("error-title")
                }
                Spacer()
                Text("\(form.draft.title.count)/\(Theme.titleMax)")
                    .foregroundStyle(Theme.onSurfaceVariant)
                    .accessibilityIdentifier("title-counter")
            }
            .font(.caption.weight(.medium))
        }
    }

    private func descriptionField(_ form: FormState) -> some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xs) {
            Text("Description").font(.caption.weight(.medium)).foregroundStyle(Theme.onSurfaceVariant)
            TextField("Description", text: draftBinding(\.description, form), axis: .vertical)
                .lineLimit(3...3)
                .font(.system(size: 15))
                .inputStyle(hasError: form.serverErrors["description"] != nil)
                .accessibilityIdentifier("field-description")
            fieldError(form.serverErrors["description"], id: "error-description")
        }
    }

    private func priorityField(_ form: FormState) -> some View {
        VStack(alignment: .leading, spacing: Theme.Spacing.xs) {
            Text("Priority").font(.caption.weight(.medium)).foregroundStyle(Theme.onSurfaceVariant)
            Picker("Priority", selection: draftBinding(\.priority, form)) {
                ForEach(Priority.allCases, id: \.self) { Text($0.label).tag($0) }
            }
            .pickerStyle(.segmented)
            .accessibilityIdentifier("field-priority")
            fieldError(form.serverErrors["priority"], id: "error-priority")
        }
    }

    private func dueDateField(_ form: FormState) -> some View {
        let current = DateConversion.date(from: form.draft.dueDate) ?? Date()
        let today = DateConversion.date(from: AppEnvironment.todayString()) ?? Date()
        // An old due date stays valid on edit because the server skips the check when it is unchanged.
        let lowerBound = min(today, current)
        let binding = Binding<Date>(
            get: { current },
            set: { draftBinding(\.dueDate, form).wrappedValue = DateConversion.string(from: $0) }
        )
        return VStack(alignment: .leading, spacing: Theme.Spacing.xs) {
            Text("Due date").font(.caption.weight(.medium)).foregroundStyle(Theme.onSurfaceVariant)
            DatePicker("Due date", selection: binding, in: lowerBound..., displayedComponents: .date)
                .labelsHidden()
                .accessibilityIdentifier("field-due-date")
            fieldError(form.serverErrors["dueDate"], id: "error-due-date")
        }
    }

    @ViewBuilder
    private func fieldError(_ text: String?, id: String) -> some View {
        if let text {
            Text(text).font(.caption.weight(.medium)).foregroundStyle(Theme.error).accessibilityIdentifier(id)
        }
    }

    private func actions(_ form: FormState) -> some View {
        HStack(spacing: Theme.Spacing.md) {
            Button("Cancel") { viewModel.closeForm() }
                .foregroundStyle(Theme.secondary)
                .frame(minHeight: 48)
                .accessibilityIdentifier("form-cancel")
            Spacer()
            Button {
                Task { await viewModel.save() }
            } label: {
                HStack(spacing: Theme.Spacing.sm) {
                    if form.isSaving { ProgressView().tint(Theme.onPrimary) }
                    Text("Save")
                }
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(Theme.onPrimary)
                .padding(.horizontal, Theme.Spacing.lg)
                .frame(minHeight: 48)
                .background(Theme.primary, in: RoundedRectangle(cornerRadius: Theme.Radius.md))
            }
            .disabled(form.isSaving || form.titleError != nil)
            .opacity(form.isSaving || form.titleError != nil ? 0.38 : 1)
            .accessibilityIdentifier("form-save")
        }
        .padding(Theme.Spacing.md)
        .background(Theme.surface)
    }
}

private extension View {
    func inputStyle(hasError: Bool) -> some View {
        padding(Theme.Spacing.sm + Theme.Spacing.xs)
            .frame(minHeight: Theme.minTouchTarget)
            .background(Theme.surfaceVariant, in: RoundedRectangle(cornerRadius: Theme.Radius.md))
            .overlay(
                RoundedRectangle(cornerRadius: Theme.Radius.md)
                    .stroke(hasError ? Theme.error : Theme.outlineStrong, lineWidth: hasError ? 2 : 1)
            )
    }
}
