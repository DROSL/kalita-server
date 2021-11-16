#!flask/bin/python
import os
import io
import hashlib
import sqlite3
import os
import spacy
import wave

from datetime import datetime
from flask import Flask, request, send_file
from TTS.utils.synthesizer import Synthesizer
from TTS.utils.manage import ModelManager
from urllib.parse import unquote

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

	synthesizer = Synthesizer(model_args['tts_checkpoint'], model_args['tts_config'], None, model_args['vocoder_checkpoint'], model_args['vocoder_config'])
	return synthesizer

path = os.path.join(os.getcwd(), ".models.json")
manager = ModelManager(path)

languages = ['german', 'english', 'french']

german_args = {'model_name': 'tts_models/de/thorsten/tacotron2-DCA', 'vocoder_name': 'vocoder_models/de/thorsten/fullband-melgan'}
english_args = {'model_name': 'tts_models/en/ljspeech/speedy-speech', 'vocoder_name': 'vocoder_models/en/ljspeech/hifigan_v2'}
french_args = {'model_name': 'tts_models/fr/mai/tacotron2-DDC', 'vocoder_name': 'vocoder_models/universal/libri-tts/wavegrad'}

for x in languages:
	globals()[x] = create_synthesizer(globals()[x + '_args'])

app = Flask(__name__)

def save_request(time, ip_hash, text_hash, audio_hash):
	database = sqlite3.connect('requests.db')
	cursor = database.cursor()

	cursor.execute("""SELECT count(name) FROM sqlite_master WHERE type='table' AND name='requests'""")
	if(cursor.fetchone()[0] == 0): 
		cursor.execute("""CREATE TABLE requests (time, ip, text, audio)""")

	cursor.execute("""INSERT INTO requests VALUES (?,?,?,?)""", (time, ip_hash, text_hash, audio_hash))

	database.commit()
	database.close()

@app.route('/api/tts', methods=['GET'])
def tts():
	time = datetime.now().strftime('%d.%m.%Y - %H:%M:%S')
	ip_hash = hashlib.sha256(request.access_route[0].encode('utf-8')).hexdigest()

	files = []

	text = unquote(request.args.get('text') + ".")
	nlp = spacy.load("de_core_news_sm")
	doc = nlp(text)

	if(request.args.get('language')):
		language = request.args.get('language')
	else:
		language = "german"

	full_text_hash = hashlib.sha256(text.encode('utf-8')).hexdigest()

	assert doc.has_annotation("SENT_START")
	for sent in doc.sents:
		text = sent.text
		text_hash = hashlib.sha256(text.encode('utf-8')).hexdigest()
		file_path = os.path.join(os.getcwd(), "files", text_hash)

		if(os.path.isfile(file_path)):
			file = open(file_path, "rb")
			out = io.BytesIO(file.read())
			file.close()
			files.append(file_path)
			out.seek(0)
		else:
			synthesizer = globals()[language]
			wavs = synthesizer.tts(text)
			out = io.BytesIO()
			synthesizer.save_wav(wavs, out)
			file = open(file_path, "wb")
			file.write(out.getbuffer())
			file.close()
			files.append(file_path)

		audio_hash = hashlib.sha256(repr(out).encode('utf-8')).hexdigest()

		save_request(time, ip_hash, text_hash, audio_hash)

	data= []
	for file in files:
		w = wave.open(file, 'rb')
		data.append( [w.getparams(), w.readframes(w.getnframes())] )
		w.close()

	output_path = os.path.join(os.getcwd(), "files", full_text_hash)

	output = wave.open(output_path, 'wb')
	output.setparams(data[0][0])
	for i in range(len(data)):
		output.writeframes(data[i][1])
	output.close()
	file = open(output_path, "rb")
	out = io.BytesIO(file.read())
	file.close()
	os.remove(output_path)

	return send_file(out, mimetype='audio/wav')


def main():
	python_server_address = "0.0.0.0"
	python_port = 1337
	with open("config.properties") as f:
		l = [line.split("=") for line in f.readlines()]
		for key, value in l:
			if(key == "python_server_address"):
				python_server_address = str(value.strip())
			if(key == "python_port"):
				python_port = int(value.strip())
	f.close()
	app.run(debug=False, host=python_server_address, port=python_port)


if __name__ == '__main__':
	main()
