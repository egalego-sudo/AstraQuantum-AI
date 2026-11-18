# QUANTUM AI — Legacy APK Recovery Record

**Creator by Bittu Pandit**  
**Recovery date:** 2026-11-18  
**Repository:** `egalego-sudo/Quantum-AI`

Seven selected historical DEBUG APK artifacts were downloaded from GitHub Actions, extracted and independently checked.

| Build | Actions artifact ID | APK size (bytes) | APK SHA-256 | Source commit |
|---:|---:|---:|---|---|
| 1 | 14395618346 | 29412770 | `4cae8729f30be64b01855d075d7b0107af408d79e8e45ed102e91087a6950eb8` | `bd7b4357efe7455d43df0b70c7cd5cfb6570d992` |
| 2 | 14395693754 | 29412770 | `3d0c67809a3ab71eac5cc2d5d671d58e99cadab0b060ac6ff46fbd7ea358a076` | `a043c2b4a4b279186af6bd53cf80451409d8a201` |
| 91 | 14396387880 | 60778960 | `3b2606cda6d2f6b53a4cbb2ed52b5cee8b9b5fd5c1ba781b5ce85c01d2772850` | `914f5e277f379b3ea0de945d3fe4fc4175024a83` |
| 92 | 14396318486 | 60778960 | `d07c72c52a17c3ba9c908b8095fccec10c63b6fb6771e353d9d254fe5065fab8` | `7d641e42746e6d4c0591218ca8a72c0c878ca5b3` |
| 203 | 14396688381 | 61584759 | `33a5b1b23304e46e11e970bb3582c2506b5f1e3108e35ecad006f2fa71aafa80` | `0f1e174eb2ce3ef7b2b5b493a6c1224e568fa22b` |
| 204 | 14396548979 | 61584759 | `c071b68d0c5a3deeb9ef363d05d7c350553f432c8958f147cf804c96be2ba8a2` | `7067243578867845687d73ca7947d5055fce2b5f` |
| 207 | 14398020496 | 61584759 | `927e56fe83915f187cb0d0587575e921b086c67defd012eb941b98b57c67c7fc` | `07133cc4ae217e887a370fe59098677dcfde669f` |

## Verification

- All seven Actions artifact ZIP files downloaded successfully.
- All seven ZIP archives passed ZIP listing/extraction.
- Each extracted APK's SHA-256 was independently recomputed.
- All seven computed APK hashes exactly matched the corresponding `SHA256SUMS.txt` value embedded in that build artifact.
- The archive package includes the seven APKs, each build's `BUILD-INFO.txt` and original checksum manifest.
- Combined downloadable archive SHA-256: `8f18abeffaccbe3322ade9c95d65e667c12b2777cebf7c87dbc40f27679604f2`.
- Original downloaded Actions ZIP SHA-256 values:
  - Build 1: `cc7eb97ff7b44c24aed2e8dfadeab82d9780bf0cfb37029fd9e0eb28c24eeb31`
  - Build 2: `b6afe00bf550127f930eee9b0d336f3bbd14f35227809b889d847a94da3c3290`
  - Build 91: `593fe972234872559a303040fa30e97e56f9c82f6fff1ffd079191b0d0821f72`
  - Build 92: `6c199c149206ebaab88fd53081c5debc597e3f688a82321812a4358324ca5d10`
  - Build 203: `2f4dfefe1a264879b7f9249c7248c48e59125d031732a5db63716e60e4703aed`
  - Build 204: `ba61ee71e8d267c7e78fbf2b5cae2adbd2574df550d364b642a3389122abea17`
  - Build 207: `2fc9f168f2c0d005b494fa4f31c5ac16b2274d189256010a150ac0c1c4d139a4`

## Limitations

These are DEBUG APKs, not production-signed release APKs. Their package/signing/update compatibility with the current app is not certified. Do not install them over the current QUANTUM AI app. The combined ZIP was generated for user download in the conversation; it is not uploaded to a permanent GitHub Release asset. GitHub Actions artifacts have expiry dates, so the user should retain the combined archive in their own storage.

This recovery covers only the seven selected targets, not every historical APK, source patch or delta artifact for Builds 1–385.
