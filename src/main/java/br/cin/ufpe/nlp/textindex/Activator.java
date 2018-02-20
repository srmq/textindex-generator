package br.cin.ufpe.nlp.textindex;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.cin.ufpe.nlp.api.bagofwords.TfIdfComputerService;
import br.cin.ufpe.nlp.api.tokenization.Tokenizer;
import br.cin.ufpe.nlp.api.tokenization.TokenizerFactory;


public class Activator  implements BundleActivator {
	private static Logger logger = LoggerFactory.getLogger(Activator.class);
	boolean firstTime = true;
	private Map<String, Object> services = new HashMap<String, Object>(2);
	private String inputPath;
	private String outputPath;
	
	private class ServiceAvailableListener<T> implements ServiceListener {

		private BundleContext context;
		
		public ServiceAvailableListener(BundleContext context) {
			this.context = context;
		}
		
		public void serviceChanged(ServiceEvent ev) {
			if (ev.getType() != ServiceEvent.UNREGISTERING && firstTime) {
				ServiceReference servRef = ev.getServiceReference();
				@SuppressWarnings("unchecked")
				T service = (T) context.getService(servRef);
				services.put(service.getClass().getName(), service);
				logger.info(service.getClass().getName() + " became available, continuing");
				if(isReady()) {
					try {
						doStuff();
					} catch (IOException e) {
						throw new IllegalStateException("IOException when trying doStuff following serviceChanged", e);
					}
					firstTime = false;
				}
			}
			
		}
	}
	
	private void doStuff() throws IOException {
		TfIdfComputerService tfIdfService = (TfIdfComputerService) services.get(TfIdfComputerService.class.getName());
		@SuppressWarnings("unchecked")
		TokenizerFactory<Tokenizer<String>> tokenizer = (TokenizerFactory<Tokenizer<String>>) services.get(TokenizerFactory.class.getName()); 
		TextindexController controller = new TextindexController(this.inputPath, this.outputPath, tfIdfService, tokenizer);
		controller.process();
	}
	
	private boolean isReady() {
		return services.containsKey(TfIdfComputerService.class.getName()) && services.containsKey(TokenizerFactory.class.getName());
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public void start(BundleContext context) throws Exception {
		inputPath = System.getenv("TEXTINDEX_INPUTPATH");
		if (inputPath == null) {
			throw new IllegalArgumentException("Missing environment variable TEXTINDEX_INPUTPATH");
		}
		outputPath = System.getenv("TEXTINDEX_OUTPUTPATH");
		if (outputPath == null) {
			throw new IllegalArgumentException("Missing environment variable TEXTINDEX_OUTPUTPATH");
		}
		
		ServiceReference tfRef = context.getServiceReference(TfIdfComputerService.class.getName());
		if (tfRef == null) {
			logger.warn("TfIdfComputerService service not found, waiting for it");
			context.addServiceListener(new ServiceAvailableListener<TfIdfComputerService>(context), "(" + Constants.OBJECTCLASS + "=" + TfIdfComputerService.class.getName() + ")");
		} else {
			logger.info("TfIdfComputerService available, continuing");
			TfIdfComputerService tfIdfServ = (TfIdfComputerService) context.getService(tfRef);
			services.put(TfIdfComputerService.class.getName(), tfIdfServ);
		}
		
		ServiceReference[] tokenservices = context.getServiceReferences(TokenizerFactory.class.getName(), "(type=line)");
		if (tokenservices == null) {
			logger.warn("LineTokenizer not found, waiting for it...");
			context.addServiceListener(new ServiceAvailableListener<TokenizerFactory>(context), "(&(" + Constants.OBJECTCLASS + "=" + TokenizerFactory.class.getName() + ")(type=line))");			
		} else {
			@SuppressWarnings("unchecked")
			TokenizerFactory<Tokenizer<String>> tokenizerFactory = (TokenizerFactory<Tokenizer<String>>) context.getService(tokenservices[0]);
			services.put(TokenizerFactory.class.getName(), tokenizerFactory);
			if (isReady()) {
				doStuff();
				firstTime = false;
			}
		}

	}

	@Override
	public void stop(BundleContext arg0) throws Exception {
		
	}


}
