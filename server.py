#!flask/bin/python
import os
import io

from flask import Flask, request, send_file
from TTS.utils.synthesizer import Synthesizer
from TTS.utils.manage import ModelManager

def create_synthesizer(model_args):
	if model_args['model_name'] is not None:
		tts_checkpoint_file, tts_config_file, tts_json_dict = manager.download_model(model_args['model_name'])
	
	if model_args['vocoder_name'] is None:
		model_args['vocoder_name'] = tts_json_dict['default_vocoder']

	if model_args['vocoder_name'] is not None:
		vocoder_checkpoint_file, vocoder_config_file, vocoder_json_dict = manager.download_model(model_args['vocoder_name'])

	if os.path.isfile(tts_checkpoint_file):
		model_args['tts_checkpoint'] = tts_checkpoint_file
	if os.path.isfile(tts_config_file):
		model_args['tts_config'] = tts_config_file

	if os.path.isfile(vocoder_checkpoint_file):
		model_args['vocoder_checkpoint'] = vocoder_checkpoint_file
	if os.path.isfile(vocoder_config_file):
		model_args['vocoder_config'] = vocoder_config_file

	synthesizer = Synthesizer(model_args['tts_checkpoint'], model_args['tts_config'], model_args['vocoder_checkpoint'], model_args['vocoder_config'])
	return synthesizer

path = "./.models.json"
manager = ModelManager(path)

languages = ['german', 'english']

german_args = {'model_name': 'tts_models/de/thorsten/tacotron2-DCA', 'vocoder_name': 'vocoder_models/universal/libri-tts/fullband-melgan'}
english_args = {'model_name': 'tts_models/en/ljspeech/tacotron2-DCA', 'vocoder_name': 'vocoder_models/en/ljspeech/multiband-melgan'}

for x in languages:
	globals()[x] = create_synthesizer(globals()[x + '_args'])

app = Flask(__name__)

@app.route('/api/tts', methods=['GET'])
def tts():
    language = request.args.get('language')
    text = request.args.get('text')
    synthesizer = globals()[language]
    print(" > Model input: {}".format(text))
    wavs = synthesizer.tts(text)
    out = io.BytesIO()
    synthesizer.save_wav(wavs, out)
    return send_file(out, mimetype='audio/wav')


def main():
    app.run(debug=False, host='0.0.0.0', port=3000)


if __name__ == '__main__':
    main()
