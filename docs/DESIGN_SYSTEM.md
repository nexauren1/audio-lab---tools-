# Audio Lab Tools — design system

## Palette

| Token | Hex | Use |
|---|---|---|
| Ink | **#080B11** | App background |
| Surface 1 | **#111722** | Cards and navigation |
| Surface 2 | **#17202C** | Inputs and nested panels |
| Stroke | **#263242** | Borders and separators |
| Mint | **#79F2D0** | Primary action and active state |
| Lavender | **#A7A6FF** | Voice and AI category |
| Sky | **#88CEFF** | Edit and convert category |
| Amber | **#FFC878** | Create and analyze category |
| Rose | **#FF91BE** | Experimental tools and saved state |

## Layout

- Screen horizontal gutter: 18dp
- Main content spacing: 16dp
- Tool grid spacing: 12dp
- Main cards: 20–26dp corner radius
- Search field: 17dp corner radius
- Tool-card minimum height: 164dp
- Bottom navigation uses three destinations: Explore, Saved, Settings

## Interaction states

- Selected category: mint fill with dark text.
- Unselected category: deep surface with muted text.
- Tool category marker uses the accent belonging to that category.
- Saved tools use a rose-filled heart.
- Search filters by localized name and description.
- Opening a tool shows a dedicated workspace sheet and lets the user pick an audio/video file with Android's document picker.

## App icon

The app icon is an original waveform mark in mint and lavender on a dark rounded-square background. The SVG source lives at artwork/audio-lab-icon.svg, and the Android adaptive icon uses the vector drawable in app/src/main/res.
