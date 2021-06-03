#kalita-server

## 🔗 Links and Resources
| Type                            | Links                               |
| ------------------------------- | --------------------------------------- |
| 💾 **Installation**               | [TTS/README.md](https://github.com/azmke/kalita-server#install-tts)|

## Install TTS
Kalita is tested on Windows 10 with **python 3.8.9**.

```bash
pip install TTS
```

The underlying coquiTTS uses ```espeak-ng``` to convert graphemes to phonemes. You might need to install separately [HERE](http://espeak.sourceforge.net/download.html). Or on Linux distributions via console:

```bash
sudo apt-get install espeak-ng
```

## Using the server

After the installation, Kalita provides a CLI interface for synthesizing speech as a server.

```bash
python server.py
```

After the server started you can send requests to the server.

```bash
GET http://localhost:3000/api/tts?text=Here%20comes%20your%20text&language=english
```