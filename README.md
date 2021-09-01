<p align="center">
  <img src="https://image.flaticon.com/icons/png/512/2334/2334268.png" alt="Logo Kalita" width="200">
</p>

# kalita-server

**Kalita** a is text-to-speech software with a special focus on data minimization and user privacy. We do not collect any personal data, do not set tracking cookies and do not outsource our service to third-party cloud solutions. The speech synthesis takes place on-premises on your own server and still offers many of the conveniences of a conventional readspeaker.

- [**kalita-server**](https://github.com/azmke/kalita-server) is a server written in Python that provides the speech synthesis.
- [**kalita-js**](https://github.com/azmke/kalita-js) is a JavaScript client for integration into a website that provides a graphical user interface.

## 🔗 Links and Resources
| Type                            | Links                               |
| ------------------------------- | --------------------------------------- |
| 💾 **Installation**               | [TTS/README.md](https://github.com/azmke/kalita-server#install-tts)|

## Install TTS
Kalita is tested on Windows 10 with **python 3.8.9**.
Run the **setup.bat** file or follow these steps;

```bash
> pip install TTS
```

The underlying coquiTTS uses ```espeak-ng``` to convert graphemes to phonemes. You might need to install separately [HERE](http://espeak.sourceforge.net/download.html). Or on Linux distributions via console:

```bash
> sudo apt-get install espeak-ng
```

## Setup Java Server

```bash
> gradle wrapper
> gradlew shadowJar
```

## Using the server

After the installation, Kalita provides a CLI interface for synthesizing speech as a server.

```bash
> start.bat
```

After the server started you can send requests to the server.

```bash
> GET http://localhost:3000/api/tts?text=Here%20comes%20your%20text&language=english
```
