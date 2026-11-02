```yaml
name: "Task Manager Mobile"
description: "A calm, high-contrast design system for a single-user task list on Android (Material 3) and iOS (SwiftUI)."

colors:
  primary: "#0B6E5F"
  on-primary: "#FFFFFF"
  secondary: "#4A5568"
  surface: "#FAFAF7"
  on-surface: "#1C1B1A"
  surface-variant: "#F0EFEA"
  on-surface-variant: "#4D4A45"
  outline: "#CFCBC3"
  outline-strong: "#857F75"
  error: "#B3261E"
  on-error: "#FFFFFF"
  error-container: "#FCE8E6"
  success: "#1B6E3A"
  warning: "#8A5A00"
  info: "#1F5FA8"
  dark-primary: "#5CCFB9"
  dark-surface: "#141413"
  dark-on-surface: "#ECEAE4"
  dark-surface-variant: "#232321"
  dark-outline: "#4A4843"
  dark-error: "#F2B8B5"
  dark-error-container: "#4A1D1A"

typography:
  headline-lg:
    fontFamily: System (Roboto on Android, SF Pro on iOS)
    fontSize: 28
    fontWeight: 700
  title-md:
    fontFamily: System
    fontSize: 17
    fontWeight: 600
  body-md:
    fontFamily: System
    fontSize: 15
    fontWeight: 400
  label-sm:
    fontFamily: System
    fontSize: 12
    fontWeight: 500

rounded:
  none: 0
  sm: 4
  md: 8
  lg: 16
  full: 9999

spacing:
  xs: 4
  sm: 8
  md: 16
  lg: 24
  xl: 32
```

# Design System: Task Manager Mobile

Units are dp on Android and pt on iOS. Both apps use the same tokens.

## 1. Visual Theme & Atmosphere

Calm, flat and text-first. The screen shows one thing at a time: the list. A warm off-white surface and a deep green primary color give the app its own look and avoid the default purple template. Overdue is the only loud element. Red appears only for overdue tasks and errors, so it keeps its meaning.

## 2. Color Palette & Roles

- **Primary** `#0B6E5F`: the add button, the selected filter chip, the Save button, links.
- **Secondary** `#4A5568`: Cancel buttons, secondary text, icons.
- **Surface** `#FAFAF7`: screen background.
- **Surface Variant** `#F0EFEA`: the summary panel, unselected chips, input fill.
- **Outline** `#CFCBC3`: dividers only (decorative).
- **Outline Strong** `#857F75`: input borders and unselected chip borders (3.8:1 on surface, meets the 3:1 rule for controls).
- **Error** `#B3261E` on **Error Container** `#FCE8E6`: overdue label, overdue row accent, field errors, the Delete button.
- **Success** `#1B6E3A`: the Done status label.
- **Warning** `#8A5A00`: the In Progress status label. Priority labels use `on-surface`. High is bold, Medium is regular, Low is `on-surface-variant`.
- **Info** `#1F5FA8`: the To Do status label.
- Dark mode uses the `dark-*` tokens. Android follows the system setting. iOS follows the system setting. Do not use Android dynamic color, so both apps match.
- All text and background pairs must reach WCAG AA (4.5:1 for body text). Checked pairs: primary on surface 5.9, on-primary on primary 6.2, on-surface on surface 16.4, error on surface 6.3, error on error-container 5.5, on-error on error 6.5, success on surface 6.0, warning on surface 5.7, info on surface 6.2, and each status color on surface-variant 5.1 or higher.
- Never use color alone. Status, priority and overdue always show a text label too.

## 3. Typography Rules

- **Headline (LG)** 28 / 700: the summary total number.
- **Title (MD)** 17 / 600: task title, screen title, dialog title.
- **Body (MD)** 15 / 400: description, form input text, dialog text.
- **Label (SM)** 12 / 500: chip text, status, priority, due date, summary captions.
- Use the system font only. Line height is 1.4 for body text. Task title in the list uses at most 2 lines with an ellipsis.
- Support dynamic type (iOS) and font scale up to 200 percent (Android). Layouts must wrap, not clip.

## 4. Component Stylings

- **Primary button / FAB**: filled `primary`, `on-primary` text, radius `full` for the FAB and `md` for form buttons, minimum height 48. Pressed state darkens 12 percent. Disabled state uses 38 percent opacity.
- **Secondary button**: text-only in `secondary`, minimum height 48.
- **Destructive button** (Delete in the dialog): filled `error`, `on-error` text.
- **Input**: filled with `surface-variant`, 1 px `outline-strong` border, radius `md`. Focus border is 2 px `primary`. Error border is 2 px `error` with the message below in `label-sm` `error`. A character counter shows `n/100` under the title input.
- **Filter chip**: height 40, radius `full`, label-sm. Unselected: `surface-variant` fill, `outline-strong` border. Selected: `primary` fill, `on-primary` text, plus a check mark. Two chip rows: Status then Priority. Each row starts with an `All` chip.
- **Task row**: no card, no shadow. A row is `surface` with a 1 px `outline` divider. Padding `md`. Overdue row: `error-container` fill and an `Overdue` label in `error` bold with a warning icon.
- **Status control**: a text button on the row showing the current status. Tap opens a menu with the three statuses. The current status shows a check mark.
- **Summary panel**: `surface-variant` fill, radius `lg`, padding `md`. One big Total number, then three equal columns To Do, In Progress, Done (label-sm caption below each number). Below them one Overdue line. When overdue is above 0 the line is `error`. When it is 0 the line is `secondary`.
- **Dialog**: radius `lg`. Title, one sentence, Cancel (secondary) and Delete (destructive).
- **Snackbar / toast**: bottom, 3 seconds, `on-surface` fill with `surface` text. Used for "Task created", "Task updated", "Task deleted".

## 5. Layout Principles

- 8-point grid. Spacing tokens only: 4, 8, 16, 24, 32. No other values.
- Screen padding is `md` (16). Space between the summary panel, chip rows and list is `md`.
- Minimum touch target is 48 dp on Android and 44 pt on iOS.
- One column. The list scrolls. The summary panel and the chips scroll away with the list (they are the list header).
- Line length is not an issue on a phone. Tablets keep one column with a maximum width of 640.

## 6. Depth & Elevation

- Flat. No card shadows. Separation comes from dividers, fill and spacing.
- The FAB is the only elevated item (Material default on Android, a soft shadow of 4 pt blur on iOS).
- Dialogs and menus use the platform default scrim and shadow.
- The top bar is flat with a 1 px bottom `outline` only when the list is scrolled.

## 7. Do's and Don'ts

- **Do** show the overdue state with a red fill, a warning icon and the word `Overdue`.
- **Do** show an empty state with one sentence and, when filters are active, a `Clear filters` button.
- **Do** show field errors under the field, in the field's own position, from the API `fields` map.
- **Do** show a loading indicator on first load and keep old data on screen during a refresh.
- **Do** write labels in sentence case: `To Do`, `In Progress`, `Done`, `Low`, `Medium`, `High`.
- **Don't** use red for anything except overdue and errors.
- **Don't** put color-only meaning in the UI.
- **Don't** use card side bars, gradients, or decorative icons.
- **Don't** use em dashes in UI text.
- **Don't** add settings, tabs, or a navigation drawer. The app has two screens.

## 8. Responsive Behavior

- **Phone portrait (default)**: as described above.
- **Phone landscape**: same single column. The form scrolls. Keyboard must not hide the focused input (`adjustResize` on Android, default scroll avoidance on iOS).
- **Tablet (width above 600)**: center the content with a maximum width of 640.
- Safe areas: draw behind the system bars (edge to edge) and pad content with insets.

## 9. Interactivity

- **Press**: ripple on Android, highlight on iOS. Do not remove it.
- **Focus**: 2 px `primary` outline on inputs and chips for keyboard and switch access.
- **Loading**: a circular indicator in the center on first load. A thin linear indicator at the top for refresh. The Save button shows a small spinner and is disabled while saving.
- **Optimistic status change**: the row updates at once. On failure the row reverts and a snackbar shows the error.
- **Animation**: 150 to 200 ms ease-out for chip selection, row removal and the snackbar. Respect the system reduce-motion setting.
- **Accessibility**: every icon button has a content description. Rows read as `<title>, <priority> priority, <status>, due <date>, overdue` in one announcement. Dialog focus starts on Cancel.

## 10. Screens design system

- **Task list (home)**: top bar with the title `Tasks`. Header content in this order: summary panel, status chips, priority chips. Then rows sorted as the API returns them. The FAB sits at the bottom right (Android) and a bottom-right circular `+` button (iOS). Empty states: with no tasks at all, `No tasks yet. Tap + to add one.` With filters active and no match, `No tasks match these filters.` and a `Clear filters` button.
- **Task row**: line 1 is the title (title-md). Line 2 is the priority label, the status button and the due date (label-sm), separated by `md` spacing. The overdue row adds the `Overdue` label after the due date. A delete icon button sits at the end of the row on both apps. Tap the row body to edit.
- **Create / Edit form**: full screen. Top bar has a Close button and the title `New task` or `Edit task`. Fields in order: Title, Description (multi-line, 3 lines), Priority (three segmented buttons, default Medium on create), Due date (a field that opens the platform date picker; past dates are disabled; a new task starts with today's date). The bottom has Cancel and Save. The edit form fills all fields from the task.
- **Delete confirmation**: dialog titled `Delete task?` with the task title in the body and the text `This cannot be undone.` Buttons: Cancel and Delete.
- **Error state (network)**: replaces the list area. Text `Cannot reach the server.` and a `Try again` button. The summary panel is hidden.

## Agent prompt guide

- Use only the tokens in the YAML block. Do not invent colors or spacing values.
- Android: build a Material 3 `ColorScheme` from these tokens in `ui/theme`. Do not call `dynamicLightColorScheme`.
- iOS: define the same tokens as named colors in `Assets.xcassets` (light and dark) and one `Theme` type for spacing and radius.
- Write UI text in simple English with short sentences.
- Check each new screen against the Do's and Don'ts before you finish.
