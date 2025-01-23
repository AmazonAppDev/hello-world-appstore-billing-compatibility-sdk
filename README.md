# Hello Pizza

This project is a simple demonstration of the [Amazon Appstore Billing Compatibility SDK](https://developer.amazon.com/docs/in-app-purchasing/appstore-billing-compatibility.html) on a Amazon Fire Tablet. This SDK supports consumable, entitlement, and subscription IAPs in the same way of Google Play Billing Library versions 4.0 and 5.0.

## Prerequisite
- An Android app that uses Google Play Billing Library version 4.0 or 5.0
- Amazon Developer Console account
- Appstore Billing Compatibility SDK (download [here](https://developer.amazon.com/docs/in-app-purchasing/appstore-billing-compatibility.html#download))

## ✅ Features
Key features include:
- Buying a pizza 🍕 (consumable)
- Buying a discount
- Buying subscription 

## 💻 Building the app

1. Clone the demo app repository:
`git clone https://github.com/AmazonAppDev/hello-world-appstore-billing-compatibility-sdk.git`
2. Open the project in Android Studio and wait for Gradle to sync. ⏳
3. Log into the Amazon Developer console and create an app
4. Generate a public key for your app and download the AppstoreAuthenticationKey.pem file. More info [here](https://developer.amazon.com/docs/in-app-purchasing/appstore-billing-compatibility.html#configure-public-key)
5. Copy the PEM file to your app's ```src/main/assets folder```.
6. Add the IAPs in the Amazon Appstore console. ( Make sure the SKU are the same as in ```HelloPizzaRepository.kt``` )
7. Connect a Fire tablet via USB. See instructions for 🔌 [Connecting to Fire Device through ADB](https://developer.amazon.com/docs/fire-tablets/connecting-adb-to-device.html)
8. Choose 'Run' > 'Run app' to launch the app on your device or emulator. ▶️

## Get support
If you found a bug or want to suggest a new [feature/use case/sample], please [file an issue](../../issues).

If you have questions, comments, or need help with code, we're here to help:
- on X at [@AmazonAppDev](https://twitter.com/AmazonAppDev)
- on Stack Overflow at the [amazon-appstore](https://stackoverflow.com/questions/tagged/amazon-appstore) tag

### Stay updated
Get the most up to date Amazon Appstore developer news, product releases, tutorials, and more:

* 📣 Follow [@AmazonAppDev](https://twitter.com/AmazonAppDev) and [our team](https://twitter.com/i/lists/1580293569897984000) on [Twitter](https://twitter.com/AmazonAppDev)

* 📺 Subscribe to our [Youtube channel](https://www.youtube.com/amazonappstoredevelopers)

* 📧 Sign up for the [Developer Newsletter](https://m.amazonappservices.com/devto-newsletter-subscribe)

## Authors

- [@giolaq](https://twitter.com/giolaq)
- [@_yoolivia](https://twitter.com/_yoolivia)

