package br.cin.ufpe.nlp.textindex;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Locale;
import java.util.Optional;

import br.cin.ufpe.nlp.api.bagofwords.TfIdfInfo;
import br.cin.ufpe.nlp.api.tokenization.TokenPreProcess;
import br.cin.ufpe.nlp.api.tokenization.Tokenizer;
import br.cin.ufpe.nlp.api.tokenization.TokenizerFactory;
import br.cin.ufpe.nlp.api.transform.DocumentProcessor;

public class DocToIndicesProcessor implements DocumentProcessor {
	private TokenizerFactory<Tokenizer<String>> tokenizer;
	private TfIdfInfo tfIdfInfo;
	private Optional<Long> unkWordId;
	
	private static class LowerCasePreProcessor implements TokenPreProcess {

		@Override
		public String preProcess(String token) {
			String ret = null;
			if (token != null) {
				ret = token.toLowerCase(Locale.ENGLISH);
			}
			return ret;
		}
		
	}
	
	private static TokenPreProcess myPreprocessor = new LowerCasePreProcessor(); 

	public DocToIndicesProcessor(TokenizerFactory<Tokenizer<String>> tokenizer, TfIdfInfo tfIdfInfo, long unkWordId) {
		this(tokenizer, tfIdfInfo);
		this.unkWordId = Optional.of(unkWordId);
	}
	
	public DocToIndicesProcessor(TokenizerFactory<Tokenizer<String>> tokenizer, TfIdfInfo tfIdfInfo) {
		this.tokenizer = tokenizer;
		this.tfIdfInfo = tfIdfInfo;
		this.unkWordId = Optional.empty();
	}

	@Override
	public void processDocument(Reader inputDocument, Writer outputDocument) throws IOException {
		Tokenizer<String> myTok = tokenizer.create(inputDocument);
		myTok.setTokenPreProcessor(myPreprocessor);
		while(myTok.hasMoreTokens()) {
			final String token = myTok.nextToken();
			long idWord = tfIdfInfo.indexOf(token);
			if (idWord == -1) {
				if (unkWordId.isPresent()) {
					idWord = unkWordId.get();
				} else {
					throw new IllegalStateException("Could not find " + token + " in the vocabulary and does not have an index for unknown words");
				}
			}
			outputDocument.append(Long.toString(idWord+1)); //indices are 0-based, writing as 1-based
			outputDocument.append('\n');
		}
	}

}
