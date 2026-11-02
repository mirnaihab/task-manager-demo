import SwiftUI

struct TaskRowView: View {
    let task: TaskItem
    let onEdit: () -> Void
    let onStatus: (Status) -> Void
    let onDelete: () -> Void

    var body: some View {
        HStack(alignment: .top, spacing: Theme.Spacing.sm) {
            VStack(alignment: .leading, spacing: Theme.Spacing.sm) {
                Button(action: onEdit) {
                    Text(task.title)
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundStyle(Theme.onSurface)
                        .lineLimit(2)
                        .multilineTextAlignment(.leading)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                .buttonStyle(.plain)
                .accessibilityLabel(rowLabel)
                .accessibilityHint("Edit task")
                .accessibilityIdentifier("task-title-\(task.id)")
                details
            }
            Button(action: onDelete) {
                Image(systemName: "trash")
                    .foregroundStyle(Theme.secondary)
                    .frame(minWidth: Theme.minTouchTarget, minHeight: Theme.minTouchTarget)
            }
            .accessibilityLabel("Delete task")
            .accessibilityIdentifier("delete-\(task.id)")
        }
        .padding(.vertical, Theme.Spacing.md)
        .padding(.horizontal, Theme.Spacing.md)
        .background(task.overdue ? Theme.errorContainer : Theme.surface)
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier("task-row-\(task.id)")
    }

    private var details: some View {
        // Wraps to a second line at large text sizes.
        ViewThatFits(in: .horizontal) {
            HStack(spacing: Theme.Spacing.md) { detailItems }
            VStack(alignment: .leading, spacing: Theme.Spacing.xs) { detailItems }
        }
    }

    @ViewBuilder
    private var detailItems: some View {
        Text(task.priority.label)
            .font(.caption.weight(task.priority == .high ? .bold : .medium))
            .foregroundStyle(task.priority == .low ? Theme.onSurfaceVariant : Theme.onSurface)
        statusMenu
        Text(task.dueDate)
            .font(.caption.weight(.medium))
            .foregroundStyle(Theme.onSurfaceVariant)
        if task.overdue {
            Label("Overdue", systemImage: "exclamationmark.triangle.fill")
                .font(.caption.weight(.bold))
                .foregroundStyle(Theme.error)
                .accessibilityIdentifier("overdue-\(task.id)")
        }
    }

    private var statusMenu: some View {
        Menu {
            ForEach(Status.allCases, id: \.self) { status in
                Button {
                    onStatus(status)
                } label: {
                    if status == task.status {
                        Label(status.label, systemImage: "checkmark")
                    } else {
                        Text(status.label)
                    }
                }
                .accessibilityIdentifier("status-option-\(status.rawValue)")
            }
        } label: {
            Text(task.status.label)
                .font(.caption.weight(.medium))
                .foregroundStyle(Theme.color(for: task.status))
                .frame(minHeight: Theme.minTouchTarget)
        }
        .accessibilityLabel("Status: \(task.status.label)")
        .accessibilityIdentifier("status-menu-\(task.id)")
    }

    private var rowLabel: String {
        let base = "\(task.title), \(task.priority.label) priority, \(task.status.label), due \(task.dueDate)"
        return task.overdue ? base + ", overdue" : base
    }
}
