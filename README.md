# 📸 ImageUploader – Kotlin Android App

![Kotlin](https://img.shields.io/badge/Kotlin-1.9-blue?logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-13-brightgreen?logo=android&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-yellow)

A **modern Android application** built with **Jetpack Compose** and **Clean Architecture** that allows users to **select, preview, and upload images** with **real-time progress tracking**.

---

## 🌟 Features

- 🖼️ **Gallery Access** – Browse and select images from your device.  
- ✨ **Multiple Selection** – Select multiple images at once.  
- 🚀 **Real-time Upload Progress** – Monitor the upload status with progress bars.  
- 📝 **Metadata Support** – Add captions, tags, or other metadata.  
- 🏗️ **Clean Architecture** – Organized layers: **Repository → UseCase → UI**.  
- 🎨 **Jetpack Compose UI** – Modern, reactive, and beautiful interface.  

---

## 🖌️ App Preview

<img src="https://github.com/user-attachments/assets/7e1de88a-0f21-4d99-a922-a5f8cba61d84" width="30%" />

<img src="https://github.com/user-attachments/assets/832961bf-1521-48e0-b9ca-4a5f430f8e91" width="30%" />

<img src="https://github.com/user-attachments/assets/3b2d0408-39b2-4bac-b19e-fa4102f8168f" width="30%" />

<img src="https://github.com/user-attachments/assets/1c9b8da1-8b05-4185-bd98-8abfd50d3549" width="30%" />



---

## 🛠️ Technologies Used

| Layer | Technology |
|-------|------------|
| Language | Kotlin |
| UI | Jetpack Compose |
| Architecture | Clean Architecture |
| Networking | Retrofit |
| Image Loading | Coil |
| Upload | Multipart/Form-Data |
| Async | Kotlin Coroutines & Flow |

---

## ⚡ Installation

### 

```bash
1️⃣ Clone the Repository
git clone https://github.com/thesnehdeepsingh/ImageUploader.git
cd ImageUploader
2️⃣ Open in Android Studio
Open Android Studio → File → Open → Select project folder

Let Gradle sync all dependencies

3️⃣ Configure Server (Optional)
Setup your server endpoint for image uploads

Update the base URL in the Retrofit client

4️⃣ Run the App
Connect your Android device or start an emulator

Press Run in Android Studio

📸 Usage
Launch the app on your device

Select single or multiple images from your gallery

Optionally add captions or tags

Tap Upload

Monitor progress bars for each image

🧪 Testing
Unit tests: src/test

UI tests: src/androidTest

Tools: JUnit, Espresso

💡 Tips
💡 Make sure your server supports Multipart/Form-Data
💡 Use high-resolution images for testing the upload progress
💡 Customize ImageEntity metadata to extend features like tags or locations

🛡️ License
This project is licensed under the MIT License – see the LICENSE file.

📞 Contact
Author: Snehdeep Singh

GitHub: @thesnehdeepsingh

No: 7307199126

Made with ❤️ using Kotlin & Jetpack Compose
