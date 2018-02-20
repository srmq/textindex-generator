package br.cin.ufpe.nlp.textindex;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

import br.cin.ufpe.nlp.api.bagofwords.TfIdfComputerService;
import br.cin.ufpe.nlp.api.bagofwords.TfIdfInfo;
import br.cin.ufpe.nlp.api.tokenization.Tokenizer;
import br.cin.ufpe.nlp.api.tokenization.TokenizerFactory;
import br.cin.ufpe.nlp.api.transform.DocumentProcessor;
import br.cin.ufpe.nlp.util.RecursiveTransformer;

public class TextindexController {
	private String inputPath;
	private String outputPath;
	private TfIdfComputerService tfIdfService;
	private TokenizerFactory<Tokenizer<String>> tokenizer;

	public TextindexController(String inputPath, String outputPath, TfIdfComputerService tfIdfService, TokenizerFactory<Tokenizer<String>> tokenizer) {
		this.inputPath = inputPath;
		this.outputPath = outputPath;
		this.tfIdfService = tfIdfService;
		this.tokenizer = tokenizer; 
	}
	
	public void process() throws IOException {
		TfIdfInfo info = tfIdfService.computeTfIdfRecursively(new File(inputPath).toPath(), true);

		DocumentProcessor docProcessor = new DocToIndicesProcessor(this.tokenizer, info);
		RecursiveTransformer.recursiveProcess(new File(inputPath), new File(outputPath), docProcessor, 1.0);
		
		Collection<String> words = info.getVocabWords();
		//Collection<Long> indices = info.getIndices();
		BufferedWriter dfFile = new BufferedWriter(new FileWriter(new File(new File(outputPath), "df.txt")));
		BufferedWriter wordFile = new BufferedWriter(new FileWriter(new File(new File(outputPath), "words.lst")));
		//BufferedWriter indicesFile = new BufferedWriter(new FileWriter(new File(new File(outputPath), "indices.txt")));
		//Iterator<Long> indiceIterator = indices.iterator();
		final long ndocs = info.totalNumberOfDocs();
		dfFile.append(Long.toString(ndocs));
		dfFile.append('\n');
		for (String word : words) {
			wordFile.append(word);
			wordFile.append('\n');
			dfFile.append(Integer.toString(info.docAppearedIn(word)));
			dfFile.append('\n');
			//indicesFile.append(Long.toString(indiceIterator.next()));
			//indicesFile.append('\n');
		}
		wordFile.close();
		dfFile.close();
		//indicesFile.close();
	}

}
