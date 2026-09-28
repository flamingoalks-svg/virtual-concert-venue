# unity-client

Unity-проект клиентского приложения виртуальной площадки (Unity 6.3 LTS, сборка WebGL / WebXR).

## Как создать проект в этом каталоге

1. В Unity Hub: **New project** → шаблон **3D (URP)** → в поле *Location* укажите этот каталог репозитория (сам проект должен лежать прямо в `unity-client/`, чтобы здесь оказались папки `Assets/`, `Packages/`, `ProjectSettings/`).
2. В редакторе: **Edit → Project Settings → Editor**
   - *Version Control → Mode*: **Visible Meta Files**
   - *Asset Serialization → Mode*: **Force Text**
3. Закоммитьте только `Assets/`, `Packages/`, `ProjectSettings/` — служебные каталоги (`Library`, `Temp`, `Obj`, `Logs`, `UserSettings`, `Build`) уже исключены в `.gitignore`.
