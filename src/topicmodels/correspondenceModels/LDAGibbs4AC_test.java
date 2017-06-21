package topicmodels.correspondenceModels;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

import structures.*;
import topicmodels.languageModelBaseLine;
import utils.Utils;

public class LDAGibbs4AC_test extends LDAGibbs4AC {

	languageModelBaseLine m_LM;
	protected double m_tau;
	double m_ksi;
	
	public LDAGibbs4AC_test(int number_of_iteration, double converge,
			double beta, _Corpus c, double lambda, int number_of_topics,
			double alpha, double burnIn, int lag, double ksi, double tau) {
		super(number_of_iteration, converge, beta, c, lambda, number_of_topics,
				alpha, burnIn, lag);

		m_LM = new languageModelBaseLine(c, ksi);
		m_tau = tau;
		m_ksi = ksi;

	}

	public void printTopWords(int k, String betaFile) {

		double loglikelihood = calculate_log_likelihood();
		System.out.format("Final Log Likelihood %.3f\t", loglikelihood);

		String filePrefix = betaFile.replace("topWords.txt", "");
		debugOutput(filePrefix);

		Arrays.fill(m_sstat, 0);

		System.out.println("print top words");
		for (_Doc d : m_trainSet) {
			for (int i = 0; i < number_of_topics; i++)
				m_sstat[i] += m_logSpace ? Math.exp(d.m_topics[i])
						: d.m_topics[i];
		}

		Utils.L1Normalization(m_sstat);

		try {
			System.out.println("beta file");
			PrintWriter betaOut = new PrintWriter(new File(betaFile));
			for (int i = 0; i < topic_term_probabilty.length; i++) {
				MyPriorityQueue<_RankItem> fVector = new MyPriorityQueue<_RankItem>(
						k);
				for (int j = 0; j < vocabulary_size; j++)
					fVector.add(new _RankItem(m_corpus.getFeature(j),
							topic_term_probabilty[i][j]));

				betaOut.format("Topic %d(%.3f):\t", i, m_sstat[i]);
				for (_RankItem it : fVector) {
					betaOut.format("%s(%.3f)\t", it.m_name,
							m_logSpace ? Math.exp(it.m_value) : it.m_value);
					System.out.format("%s(%.3f)\t", it.m_name,
							m_logSpace ? Math.exp(it.m_value) : it.m_value);
				}
				betaOut.println();
				System.out.println();
			}

			betaOut.flush();
			betaOut.close();
		} catch (Exception ex) {
			System.err.print("File Not Found");
		}
	}

	public void debugOutput(String filePrefix) {

		File topicFolder = new File(filePrefix + "topicAssignment");

		if (!topicFolder.exists()) {
			System.out.println("creating directory" + topicFolder);
			topicFolder.mkdir();
		}

		File childTopKStnFolder = new File(filePrefix + "topKStn");
		if (!childTopKStnFolder.exists()) {
			System.out.println("creating top K stn directory\t"
					+ childTopKStnFolder);
			childTopKStnFolder.mkdir();
		}

		File stnTopKChildFolder = new File(filePrefix + "topKChild");
		if (!stnTopKChildFolder.exists()) {
			System.out.println("creating top K child directory\t"
					+ stnTopKChildFolder);
			stnTopKChildFolder.mkdir();
		}

		int topKStn = 10;
		int topKChild = 10;

		File parentChildTopicDistributionFolder = new File(filePrefix
				+ "topicProportion");
		if (!parentChildTopicDistributionFolder.exists()) {
			System.out.println("creating topic distribution folder\t"
					+ parentChildTopicDistributionFolder);
			parentChildTopicDistributionFolder.mkdir();
		}

		for (_Doc d : m_trainSet) {
			if (d instanceof _ParentDoc) {
				printParentTopicAssignment(d, topicFolder);
				printParameter(d, parentChildTopicDistributionFolder);

			} else if (d instanceof _ChildDoc) {
				printChildTopicAssignment(d, topicFolder);
			}
			// if(d instanceof _ParentDoc){
			// printTopKChild4Stn(topKChild, (_ParentDoc)d, stnTopKChildFolder);
			// printTopKStn4Child(topKStn, (_ParentDoc)d, childTopKStnFolder);
			// }
		}

		String parentParameterFile = filePrefix + "parentParameter.txt";
		String childParameterFile = filePrefix + "childParameter.txt";

		printParameter(parentParameterFile, childParameterFile, m_trainSet);
		// printTestParameter4Spam(filePrefix);

		String similarityFile = filePrefix + "topicSimilarity.txt";
		discoverSpecificComments(similarityFile);
		printEntropy(filePrefix);
		printTopKChild4Parent(filePrefix, topKChild);
		printTopKChild4Stn(filePrefix, topKChild);
		printTopKChild4StnWithHybrid(filePrefix, topKChild);
		printTopKChild4StnWithHybridPro(filePrefix, topKChild);
		printTopKStn4Child(filePrefix, topKStn);
		printTopicWordDistribution(filePrefix);
		int randomNum = 5;
		int topK = 5;
		selectStn(filePrefix, topK, randomNum);

	}

	protected void selectStn(String filePrefix, int topK, int randomNum){
		topK = 5;
		int docSize = m_trainSet.size();
		int[] selectedArray = new int[randomNum];
		for(int i=0; i<randomNum; i++)
			selectedArray[i]=m_rand.nextInt(500);

		System.out.println("printing sentence");
		String sentenceFile = filePrefix+"selectedStn.txt";
		String sentenceIndexFile = filePrefix + "stnIndex.txt";
		try {
			PrintWriter stnOut = new PrintWriter(new File(sentenceFile));
			PrintWriter stnIndexOut = new PrintWriter(new File(sentenceIndexFile));

			for (_Doc d : m_trainSet) {
				if (d instanceof _ParentDoc) {
					_ParentDoc4DCM pDoc = (_ParentDoc4DCM) d;
					if(pDoc.getSenetenceSize()<topK){
						continue;
					}

					ArrayList<Integer> mergedStnIndexList = new ArrayList<Integer>();
					ArrayList<Integer> normLikelihoodStnList = new ArrayList<Integer>();
					ArrayList<Integer> majorTopicStnList = new ArrayList<Integer>();
					ArrayList<Integer> parentLikelihoodStnList = new ArrayList<Integer>();

					estimateTopicProb4Words(pDoc);
					topStn4ParentByNormLikelihood(pDoc, topK, normLikelihoodStnList, mergedStnIndexList);
					topStn4ParentByMajorTopic(pDoc, topK, majorTopicStnList, mergedStnIndexList);
					topStn4ParentByParentLikelihood(pDoc, topK, parentLikelihoodStnList, mergedStnIndexList);

					stnOut.print(pDoc.getName());
					for (int stnIndex : mergedStnIndexList) {
						stnOut.print("\t"+stnIndex);
					}
					stnOut.println();

					stnIndexOut.print(pDoc.getName());
					stnIndexOut.print("\tnormLikelihood");
					for (int stnIndex : normLikelihoodStnList) {
						stnIndexOut.print("\t"+stnIndex);
					}

					stnIndexOut.print("\tmajorTopic");
					for (int stnIndex : majorTopicStnList) {
						stnIndexOut.print("\t" + stnIndex);
					}

					stnIndexOut.print("\tparentLikelihood");
					for (int stnIndex : parentLikelihoodStnList) {
						stnIndexOut.print("\t"+stnIndex);
					}
					stnIndexOut.println();
				}
			}

			stnIndexOut.flush();
			stnIndexOut.close();

			stnOut.flush();
			stnOut.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	protected void topStn4ParentByNormLikelihood(_ParentDoc4DCM pDoc, int topK, ArrayList<Integer> stnList, ArrayList<Integer> mergedStnList){

		HashMap<Integer, Double> stnNormLikelihoodMap = new HashMap<Integer, Double>();

		for (_Stn stnObj : pDoc.getSentences()) {

			double likelihood = rankStn4ParentByNormLikelihood(stnObj, pDoc);
			likelihood = likelihood/(stnObj.getLength()*1.0);
			stnNormLikelihoodMap.put(stnObj.getIndex(), likelihood);

		}

		List<Map.Entry<Integer, Double>> stnNormLikelihoodList = new ArrayList<Map.Entry<Integer, Double>>(stnNormLikelihoodMap.entrySet());

		Collections.sort(stnNormLikelihoodList, new Comparator<Map.Entry<Integer, Double>>() {
			public int compare(Map.Entry<Integer, Double> o1,
							   Map.Entry<Integer, Double> o2) {
				return (o2.getValue()).toString().compareTo(o1.getValue().toString());
			}
		});

		for(int i=0; i<topK; i++){
			int selectedKey = stnNormLikelihoodList.get(i).getKey();
			stnList.add(selectedKey);
			if(!mergedStnList.contains(selectedKey)){
				mergedStnList.add(selectedKey);
			}
		}

	}

	protected void topStn4ParentByMajorTopic(_ParentDoc4DCM pDoc, int topK, ArrayList<Integer> stnList, ArrayList<Integer> mergedStnList){
		int maxTopicIndex = 0;
		double maxTopicProportion = 0;
		for(int k=0; k<number_of_topics; k++){
			if(pDoc.m_topics[k]>maxTopicProportion) {
				maxTopicIndex = k;
				maxTopicProportion = pDoc.m_topics[k];
			}
		}

		int stnNum = 1;
		for (_Stn stnObj : pDoc.getSentences()) {

			int stnMajorTopic = rankStn4ParentByMajorTopic(stnObj, pDoc);
			if(stnMajorTopic==maxTopicIndex) {
				if(stnNum>topK)
					break;
				stnList.add(stnObj.getIndex());
				if (!mergedStnList.contains(stnObj.getIndex())) {
					mergedStnList.add(stnObj.getIndex());
				}
				stnNum += 1;
			}
		}

	}

	protected void topStn4ParentByParentLikelihood(_ParentDoc4DCM pDoc, int topK, ArrayList<Integer> stnList, ArrayList<Integer> mergedStnList){
		HashMap<Integer, Double> stnParentLikelihoodMap = new HashMap<Integer, Double>();

		for (_Stn stnObj : pDoc.getSentences()) {

			double likelihood = rankStn4ParentByParentLikelihood(stnObj, pDoc);
			stnParentLikelihoodMap.put(stnObj.getIndex(), likelihood);

		}

		List<Map.Entry<Integer, Double>> stnParentLikelihoodList = new ArrayList<Map.Entry<Integer, Double>>(stnParentLikelihoodMap.entrySet());

		Collections.sort(stnParentLikelihoodList, new Comparator<Map.Entry<Integer, Double>>() {
			public int compare(Map.Entry<Integer, Double> o1,
							   Map.Entry<Integer, Double> o2) {
				return (o2.getValue()).toString().compareTo(o1.getValue().toString());
			}
		});

		for(int i=0; i<topK; i++){
			int selectedKey = stnParentLikelihoodList.get(i).getKey();
			stnList.add(selectedKey);
			if(!mergedStnList.contains(selectedKey)){
				mergedStnList.add(selectedKey);
			}
		}
	}
	
	protected void printTopicWordDistribution(String filePrefix) {
		String topicWordFile = filePrefix + "topicWord.txt";
		try{
			PrintWriter pw = new PrintWriter(new File(topicWordFile));
			
			for (int k = 0; k < number_of_topics; k++) {
				pw.print(k + "\t");
				for (int v = 0; v < vocabulary_size; v++) {
					pw.print(m_corpus.getFeature(v) + ":"
							+ topic_term_probabilty[k][v] + "\t");
				}
				pw.println();
			}

			pw.flush();
			pw.close();
		}catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void discoverSpecificComments(String similarityFile) {
		System.out.println("topic similarity");

		try {
			PrintWriter pw = new PrintWriter(new File(similarityFile));

			for (_Doc doc : m_trainSet) {
				if (doc instanceof _ParentDoc) {
					pw.print(doc.getName() + "\t");
					double stnTopicSimilarity = 0.0;
					double docTopicSimilarity = 0.0;
					for (_ChildDoc cDoc : ((_ParentDoc) doc).m_childDocs) {
						pw.print(cDoc.getName() + ":");

						docTopicSimilarity = computeSimilarity(
								((_ParentDoc) doc).m_topics, cDoc.m_topics);
						pw.print(docTopicSimilarity);
						for (_Stn stnObj : doc.getSentences()) {

							stnTopicSimilarity = computeSimilarity(
									stnObj.m_topics, cDoc.m_topics);

							pw.print(":" + (stnObj.getIndex() + 1) + ":"
									+ stnTopicSimilarity);
						}
						pw.print("\t");
					}
					pw.println();
				}
			}
			pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	double computeSimilarity(double[] topic1, double[] topic2) {
		return Utils.cosine(topic1, topic2);
	}

	protected void printParentTopicAssignment(_Doc d, File topicFolder) {
		// System.out.println("printing topic assignment parent documents");

		String topicAssignmentFile = d.getName() + ".txt";
		try {

			PrintWriter pw = new PrintWriter(new File(topicFolder,
					topicAssignmentFile));

			for (_Stn stnObj : d.getSentences()) {
				pw.print(stnObj.getIndex() + "\t");
				for (_Word w : stnObj.getWords()) {
					int index = w.getIndex();
					int topic = w.getTopic();
					String featureName = m_corpus.getFeature(index);
					// System.out.println("test\t"+featureName+"\tdocName\t"+d.getName());
					pw.print(featureName + ":" + topic + "\t");
				}
				pw.println();
			}

			pw.flush();
			pw.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	protected void printChildTopicAssignment(_Doc d, File topicFolder) {
		// System.out.println("printing topic assignment parent documents");

		String topicAssignmentFile = d.getName() + ".txt";
		try {

			PrintWriter pw = new PrintWriter(new File(topicFolder,
					topicAssignmentFile));

			for (_Word w : d.getWords()) {
				int index = w.getIndex();
				int topic = w.getTopic();
				String featureName = m_corpus.getFeature(index);
				pw.print(featureName + ":" + topic + "\t");

			}

			pw.flush();
			pw.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	protected void printParameter(_Doc d, File parentChildTopicDistributionFolder) {
		System.out.println("printing parameter");
		String topicDistributionFile = d.getName() + ".txt";

		try {

			PrintWriter pw = new PrintWriter(new File(
					parentChildTopicDistributionFolder, topicDistributionFile));

			if (d instanceof _ParentDoc) {
				pw.print(d.getName() + "\t");
				pw.print("topicProportion\t");
				for (int k = 0; k < number_of_topics; k++) {
					pw.print(d.m_topics[k] + "\t");
				}

				pw.println();

				for (_ChildDoc cDoc : ((_ParentDoc) d).m_childDocs) {
					pw.print(cDoc.getName() + "\t");
					pw.print("topicProportion\t");
					for (int k = 0; k < number_of_topics; k++) {
						pw.print(cDoc.m_topics[k] + "\t");
					}
					pw.println();
				}
			}


			pw.flush();
			pw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void printParameter(String parentParameterFile,
			String childParameterFile, ArrayList<_Doc> docList) {
		System.out.println("printing parameter");
		try {
			System.out.println(parentParameterFile);
			System.out.println(childParameterFile);
			

			PrintWriter parentParaOut = new PrintWriter(new File(
					parentParameterFile));
			PrintWriter childParaOut = new PrintWriter(new File(
					childParameterFile));
			
			String parentMaxTopicIndexFile = parentParameterFile.replace("parentParameter.txt", "parentMaxTopicIndex.txt");
			PrintWriter parentMaxTopicPW =  new PrintWriter(new File(parentMaxTopicIndexFile));

			for (_Doc d : docList) {
				if (d instanceof _ParentDoc) {
					parentParaOut.print(d.getName() + "\t");
					parentParaOut.print("topicProportion\t");
					
					int maximumTopicIndex = 0;
					double maximumTopicProportion = 0;
					for (int k = 0; k < number_of_topics; k++) {
						parentParaOut.print(d.m_topics[k] + "\t");
						if(maximumTopicProportion < d.m_topics[k]){
							maximumTopicProportion = d.m_topics[k];
							maximumTopicIndex = k;
						}
					}
					
					parentMaxTopicPW.print(d.getName() + ":" + maximumTopicIndex+"\n");
	

					for (_Stn stnObj : d.getSentences()) {
						parentParaOut.print("sentence"
								+ (stnObj.getIndex() + 1) + "\t");
						for (int k = 0; k < number_of_topics; k++) {
							parentParaOut.print(stnObj.m_topics[k] + "\t");
						}
					}

					parentParaOut.println();

					for (_ChildDoc cDoc : ((_ParentDoc) d).m_childDocs) {
						childParaOut.print(cDoc.getName() + "\t");

						childParaOut.print("topicProportion\t");
						for (int k = 0; k < number_of_topics; k++) {
							childParaOut.print(cDoc.m_topics[k] + "\t");
						}

						childParaOut.println();
					}
				}
			}

			parentParaOut.flush();
			parentParaOut.close();

			parentMaxTopicPW.flush();
			parentMaxTopicPW.close();
			
			childParaOut.flush();
			childParaOut.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	protected void printEntropy(String filePrefix) {
		String entropyFile = filePrefix + "entropy.txt";
		boolean logScale = true;

		try {
			PrintWriter entropyPW = new PrintWriter(new File(entropyFile));

			for (_Doc d : m_trainSet) {
				double entropyValue = 0.0;
				entropyValue = Utils.entropy(d.m_topics, logScale);
				entropyPW.print(d.getName() + "\t" + entropyValue);
				entropyPW.println();
			}
			entropyPW.flush();
			entropyPW.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	// comment is a query, retrieve stn by topical similarity
	protected HashMap<Integer, Double> rankStn4ChildBySim(_ParentDoc pDoc,
			_ChildDoc cDoc) {

		HashMap<Integer, Double> stnSimMap = new HashMap<Integer, Double>();

		for (_Stn stnObj : pDoc.getSentences()) {
			// double stnSim = computeSimilarity(cDoc.m_topics,
			// stnObj.m_topics);
			// stnSimMap.put(stnObj.getIndex()+1, stnSim);
			//
			double stnKL = Utils.klDivergence(cDoc.m_topics, stnObj.m_topics);
			// double stnKL = Utils.KLsymmetric(cDoc.m_topics, stnObj.m_topics);
			// double stnKL = Utils.klDivergence(stnObj.m_topics,
			// cDoc.m_topics);
			stnSimMap.put(stnObj.getIndex() + 1, -stnKL);
		}

		return stnSimMap;
	}

	protected HashMap<String, Double> rankChild4StnByHybrid(_Stn stnObj,
			_ParentDoc pDoc) {
		HashMap<String, Double> childLikelihoodMap = new HashMap<String, Double>();

		double smoothingMu = m_LM.m_smoothingMu;
		for (_ChildDoc cDoc : pDoc.m_childDocs) {
			double cDocLen = cDoc.getTotalDocLength();
			_SparseFeature[] fv = cDoc.getSparse();

			double stnLogLikelihood = 0;
			double alphaDoc = smoothingMu / (smoothingMu + cDocLen);

			_SparseFeature[] sv = stnObj.getFv();
			for (_SparseFeature svWord : sv) {
				double featureLikelihood = 0;

				int wid = svWord.getIndex();
				double stnVal = svWord.getValue();

				int featureIndex = Utils.indexOf(fv, wid);
				double docVal = 0;
				if (featureIndex != -1) {
					docVal = fv[featureIndex].getValue();
				}

				double LMLikelihood = (1 - alphaDoc) * docVal / (cDocLen);

				LMLikelihood += alphaDoc * m_LM.getReferenceProb(wid);

				double TMLikelihood = 0;
				for (int k = 0; k < number_of_topics; k++) {
					// double likelihoodPerTopic =
					// topic_term_probabilty[k][wid];
					// System.out.println("likelihoodPerTopic1-----\t"+likelihoodPerTopic);
					//
					// likelihoodPerTopic *= cDoc.m_topics[k];
					// System.out.println("likelihoodPerTopic2-----\t"+likelihoodPerTopic);
					TMLikelihood += (word_topic_sstat[k][wid] / m_sstat[k])
							* (topicInDocProb(k, cDoc) / (d_alpha
									* number_of_topics + cDocLen));

					// TMLikelihood +=
					// topic_term_probabilty[k][wid]*cDoc.m_topics[k];
					// System.out.println("TMLikelihood\t"+TMLikelihood);
				}

				featureLikelihood = m_tau * LMLikelihood + (1 - m_tau)
						* TMLikelihood;
				// featureLikelihood = TMLikelihood;
				featureLikelihood = Math.log(featureLikelihood);
				stnLogLikelihood += stnVal * featureLikelihood;

			}

			childLikelihoodMap.put(cDoc.getName(), stnLogLikelihood);
		}

		return childLikelihoodMap;
	}

	protected HashMap<String, Double> rankChild4StnByHybridPro(_Stn stnObj,
			_ParentDoc pDoc) {
		HashMap<String, Double> childLikelihoodMap = new HashMap<String, Double>();

		double smoothingMu = m_LM.m_smoothingMu;
		for (_ChildDoc cDoc : pDoc.m_childDocs) {
			double cDocLen = cDoc.getTotalDocLength();

			double stnLogLikelihood = 0;
			double alphaDoc = smoothingMu / (smoothingMu + cDocLen);

			_SparseFeature[] fv = cDoc.getSparse();
			_SparseFeature[] sv = stnObj.getFv();
			for (_SparseFeature svWord : sv) {
				double wordLikelihood = 0;
				int wid = svWord.getIndex();
				double stnVal = svWord.getValue();

				int featureIndex = Utils.indexOf(fv, wid);
				double docVal = 0;
				if (featureIndex != -1) {
					docVal = fv[featureIndex].getValue();
				}

				double LMLikelihood = (1 - alphaDoc) * docVal / cDocLen;
				LMLikelihood += alphaDoc * m_LM.getReferenceProb(wid);

				double TMLikelihood = 0;

				for (int k = 0; k < number_of_topics; k++) {
					double wordPerTopicLikelihood = (word_topic_sstat[k][wid] / m_sstat[k])
							* (topicInDocProb(k, cDoc) / (d_alpha
									* number_of_topics + cDocLen));
					TMLikelihood += wordPerTopicLikelihood;
				}

				wordLikelihood = m_tau * LMLikelihood + (1 - m_tau)
						* TMLikelihood;
				wordLikelihood = Math.log(wordLikelihood);
				stnLogLikelihood += stnVal * wordLikelihood;
			}

			double cosineSim = computeSimilarity(stnObj.m_topics, cDoc.m_topics);
			stnLogLikelihood = m_tau * stnLogLikelihood + (1 - m_tau)
					* cosineSim;

			childLikelihoodMap.put(cDoc.getName(), stnLogLikelihood);
		}
		return childLikelihoodMap;
	}

	protected HashMap<String, Double> rankChild4StnByCombinatorialLikelihood(_Stn stnObj, _ParentDoc pDoc){
		HashMap<String, Double> likelihoodMap = new HashMap<String, Double>();

		for(_ChildDoc cDoc: pDoc.m_childDocs){
			double stnLogLikelihood = 0;

			double part1 = 0;
			double part2 = 0;
			double part3 = 0;

			HashMap<Integer, Double> wordNumMap = new HashMap<Integer, Double>();
			for(_Word w:cDoc.getWords()){
				int wid = w.getIndex();
				if(wordNumMap.containsKey(wid))
					wordNumMap.put(wid, wordNumMap.get(wid)+1);
				else
					wordNumMap.put(wid, 1.0);
			}

			for(_Word w:stnObj.getWords()) {
				int wid = w.getIndex();

				String wFeature = m_corpus.getFeature(wid);
				double wordNumInCMNT = 0;
				if(wordNumMap.containsKey(wid))
					wordNumInCMNT = wordNumMap.get(wid);

				part1 = wordNumInCMNT / (cDoc.getTotalDocLength() + m_ksi);
				part2 = (m_ksi * m_corpus.m_featureStat.get(wFeature).getTTF()[0]) / (m_corpus.getTotalWords() * (m_ksi + cDoc.getTotalDocLength()));
				double wordLikelihood = 0;
				for (int k = 0; k < number_of_topics; k++) {
					wordLikelihood += topic_term_probabilty[k][wid]
							* cDoc.m_topics[k];;
				}

				part3 = wordLikelihood;

				stnLogLikelihood += Math.log(m_tau * (part1 + part2) + (1 - m_tau) * (part3));
			}

			likelihoodMap.put(cDoc.getName(), stnLogLikelihood);
		}
		return likelihoodMap;
	}

	// stn is a query, retrieve comment by likelihood
	protected HashMap<String, Double> rankChild4StnByLikelihood(_Stn stnObj,
			_ParentDoc pDoc) {

		HashMap<String, Double> childLikelihoodMap = new HashMap<String, Double>();

		for (_ChildDoc cDoc : pDoc.m_childDocs) {

			double stnLogLikelihood = 0;
			for (_Word w : stnObj.getWords()) {
				double wordLikelihood = 0;
				int wid = w.getIndex();

				for (int k = 0; k < number_of_topics; k++) {
					// wordLikelihood += (word_topic_sstat[k][wid] / m_sstat[k])
					// * (topicInDocProb(k, cDoc) / (d_alpha
					// * number_of_topics + cDocLen));
					wordLikelihood += topic_term_probabilty[k][wid]
							* cDoc.m_topics[k];
				}

				stnLogLikelihood += Math.log(wordLikelihood);
			}
			childLikelihoodMap.put(cDoc.getName(), stnLogLikelihood);
		}

		return childLikelihoodMap;
	}

	protected HashMap<String, Double> rankChild4StnByLanguageModel(_Stn stnObj,
			_ParentDoc pDoc) {
		HashMap<String, Double> childLikelihoodMap = new HashMap<String, Double>();

		double smoothingMu = m_LM.m_smoothingMu;
		for (_ChildDoc cDoc : pDoc.m_childDocs) {
			int cDocLen = cDoc.getTotalDocLength();
			_SparseFeature[] fv = cDoc.getSparse();

			double stnLogLikelihood = 0;
			double alphaDoc = smoothingMu / (smoothingMu + cDocLen);

			_SparseFeature[] sv = stnObj.getFv();
			for (_SparseFeature svWord : sv) {
				double featureLikelihood = 0;

				int wid = svWord.getIndex();
				double stnVal = svWord.getValue();

				int featureIndex = Utils.indexOf(fv, wid);
				double docVal = 0;
				if (featureIndex != -1) {
					docVal = fv[featureIndex].getValue();
				}

				double smoothingProb = (1 - alphaDoc) * docVal / (cDocLen);

				smoothingProb += alphaDoc * m_LM.getReferenceProb(wid);
				featureLikelihood = Math.log(smoothingProb);
				stnLogLikelihood += stnVal * featureLikelihood;
			}

			childLikelihoodMap.put(cDoc.getName(), stnLogLikelihood);
		}

		return childLikelihoodMap;
	}

	protected HashMap<String, Double> hybridRankChild4Stn(_Stn stnObj,
			_ParentDoc pDoc) {
		HashMap<String, Double> childLikelihoodMapByTM = new HashMap<String, Double>();
		childLikelihoodMapByTM = rankChild4StnByLikelihood(stnObj, pDoc);

		HashMap<String, Double> childLikelihoodMapByLM = new HashMap<String, Double>();
		childLikelihoodMapByLM = rankChild4StnByLanguageModel(stnObj, pDoc);

		for (String cDocName : childLikelihoodMapByTM.keySet()) {
			double TMVal = childLikelihoodMapByTM.get(cDocName);
			double LMVal = childLikelihoodMapByLM.get(cDocName);
			double retrievalScore = m_tau * TMVal + (1 - m_tau) * LMVal;

			childLikelihoodMapByTM.put(cDocName, retrievalScore);
		}

		return childLikelihoodMapByTM;
	}

	protected List<Map.Entry<Integer, Double>> sortHashMap4Integer(
			HashMap<Integer, Double> stnLikelihoodMap, boolean descendOrder) {
		List<Map.Entry<Integer, Double>> sortList = new ArrayList<Map.Entry<Integer, Double>>(
				stnLikelihoodMap.entrySet());

		if (descendOrder == true) {
			Collections.sort(sortList,
					new Comparator<Map.Entry<Integer, Double>>() {
						public int compare(Entry<Integer, Double> e1,
								Entry<Integer, Double> e2) {
							return e2.getValue().compareTo(e1.getValue());
						}
					});
		} else {
			Collections.sort(sortList,
					new Comparator<Map.Entry<Integer, Double>>() {
						public int compare(Entry<Integer, Double> e1,
								Entry<Integer, Double> e2) {
							return e2.getValue().compareTo(e1.getValue());
						}
					});
		}

		return sortList;

	}

	protected List<Map.Entry<String, Double>> sortHashMap4String(
			HashMap<String, Double> stnLikelihoodMap, boolean descendOrder) {
		List<Map.Entry<String, Double>> sortList = new ArrayList<Map.Entry<String, Double>>(
				stnLikelihoodMap.entrySet());

		if (descendOrder == true) {
			Collections.sort(sortList,
					new Comparator<Map.Entry<String, Double>>() {
						public int compare(Entry<String, Double> e1,
								Entry<String, Double> e2) {
							return e2.getValue().compareTo(e1.getValue());
						}
					});
		} else {
			Collections.sort(sortList,
					new Comparator<Map.Entry<String, Double>>() {
						public int compare(Entry<String, Double> e1,
								Entry<String, Double> e2) {
							return e2.getValue().compareTo(e1.getValue());
						}
					});
		}

		return sortList;

	}

	protected void printTopKChild4Stn(int topK, _ParentDoc pDoc,
			File topKChildFolder) {
		File topKChild4PDocFolder = new File(topKChildFolder, pDoc.getName());
		if (!topKChild4PDocFolder.exists()) {
			// System.out.println("creating top K stn directory\t"+topKChild4PDocFolder);
			topKChild4PDocFolder.mkdir();
		}

		for (_Stn stnObj : pDoc.getSentences()) {
			HashMap<String, Double> likelihoodMap = rankChild4StnByLikelihood(
					stnObj, pDoc);
			String topChild4StnFile = (stnObj.getIndex() + 1) + ".txt";

			try {
				int i = 0;

				PrintWriter pw = new PrintWriter(new File(topKChild4PDocFolder,
						topChild4StnFile));

				for (Map.Entry<String, Double> e : sortHashMap4String(
						likelihoodMap, true)) {
					if (i == topK)
						break;
					pw.print(e.getKey());
					pw.print("\t" + e.getValue());
					pw.println();

					i++;
				}

				pw.flush();
				pw.close();

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	protected void printTopKStn4Child(int topK, _ParentDoc pDoc,
			File topKStnFolder) {
		File topKStn4PDocFolder = new File(topKStnFolder, pDoc.getName());
		if (!topKStn4PDocFolder.exists()) {
			// System.out.println("creating top K stn directory\t"+topKStn4PDocFolder);
			topKStn4PDocFolder.mkdir();
		}

		for (_ChildDoc cDoc : pDoc.m_childDocs) {
			String topKStn4ChildFile = cDoc.getName() + ".txt";
			HashMap<Integer, Double> stnSimMap = rankStn4ChildBySim(pDoc, cDoc);

			try {
				int i = 0;

				PrintWriter pw = new PrintWriter(new File(topKStn4PDocFolder,
						topKStn4ChildFile));

				for (Map.Entry<Integer, Double> e : sortHashMap4Integer(
						stnSimMap, true)) {
					if (i == topK)
						break;
					pw.print(e.getKey());
					pw.print("\t" + e.getValue());
					pw.println();

					i++;
				}

				pw.flush();
				pw.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	protected double rankChild4ParentByLikelihood(_ChildDoc cDoc,
			_ParentDoc pDoc) {

		int cDocLen = cDoc.getTotalDocLength();
		_SparseFeature[] fv = pDoc.getSparse();

		double docLogLikelihood = 0;
		for (_SparseFeature i : fv) {
			int wid = i.getIndex();
			double value = i.getValue();

			double wordLogLikelihood = 0;

			for (int k = 0; k < number_of_topics; k++) {
				double wordPerTopicLikelihood = (word_topic_sstat[k][wid] / m_sstat[k])
						* ((cDoc.m_sstat[k] + d_alpha) / (d_alpha
								* number_of_topics + cDocLen));
				wordLogLikelihood += wordPerTopicLikelihood;
			}

			docLogLikelihood += value * Math.log(wordLogLikelihood);
		}

		return docLogLikelihood;
	}

	protected double rankChild4ParentBySim(_ChildDoc cDoc, _ParentDoc pDoc) {
		double childSim = computeSimilarity(cDoc.m_topics, pDoc.m_topics);

		return childSim;
	}

	protected void printTopKChild4Parent(String filePrefix, int topK) {
		String topKChild4StnFile = filePrefix + "topChild4Parent.txt";
		try {
			PrintWriter pw = new PrintWriter(new File(topKChild4StnFile));

			for (_Doc d : m_trainSet) {
				if (d instanceof _ParentDoc) {
					_ParentDoc pDoc = (_ParentDoc) d;

					pw.print(pDoc.getName() + "\t");

					for (_ChildDoc cDoc : pDoc.m_childDocs) {
						double docScore = rankChild4ParentBySim(cDoc, pDoc);

						pw.print(cDoc.getName() + ":" + docScore + "\t");

					}

					pw.println();
				}
			}
			pw.flush();
			pw.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void printTopKChild4StnWithHybridPro(String filePrefix, int topK) {
		String topKChild4StnFile = filePrefix + "topChild4Stn_hybridPro.txt";
		try {
			PrintWriter pw = new PrintWriter(new File(topKChild4StnFile));

			m_LM.generateReferenceModel();

			for (_Doc d : m_trainSet) {
				if (d instanceof _ParentDoc) {
					_ParentDoc pDoc = (_ParentDoc) d;

					pw.println(pDoc.getName() + "\t" + pDoc.getSenetenceSize());

					for (_Stn stnObj : pDoc.getSentences()) {
						HashMap<String, Double> likelihoodMap = rankChild4StnByHybridPro(
								stnObj, pDoc);

						pw.print((stnObj.getIndex() + 1) + "\t");

						for (Map.Entry<String, Double> e : sortHashMap4String(
								likelihoodMap, true)) {
							pw.print(e.getKey());
							pw.print(":" + e.getValue());
							pw.print("\t");

						}
						pw.println();

					}
				}
			}
			pw.flush();
			pw.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	protected void printTopKChild4StnWithHybrid(String filePrefix, int topK) {
		String topKChild4StnFile = filePrefix + "topChild4Stn_hybrid.txt";
		try {
			PrintWriter pw = new PrintWriter(new File(topKChild4StnFile));

			m_LM.generateReferenceModel();

			for (_Doc d : m_trainSet) {
				if (d instanceof _ParentDoc) {
					_ParentDoc pDoc = (_ParentDoc) d;

					pw.println(pDoc.getName() + "\t" + pDoc.getSenetenceSize());

					for (_Stn stnObj : pDoc.getSentences()) {
						// HashMap<String, Double> likelihoodMap =
						// rankChild4StnByLikelihood(stnObj, pDoc);
						HashMap<String, Double> likelihoodMap = rankChild4StnByHybrid(
								stnObj, pDoc);
						// HashMap<String, Double> likelihoodMap =
						// rankChild4StnByLanguageModel(stnObj, pDoc);

						int i = 0;
						pw.print((stnObj.getIndex() + 1) + "\t");

						for (Map.Entry<String, Double> e : sortHashMap4String(
								likelihoodMap, true)) {
							// if(i==topK)
							// break;
							pw.print(e.getKey());
							pw.print(":" + e.getValue());
							pw.print("\t");

							i++;
						}
						pw.println();

					}
				}
			}
			pw.flush();
			pw.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	protected void printTopKChild4Stn(String filePrefix, int topK) {
		String topKChild4StnLDAFile = filePrefix + "topChild4Stn_LDA.txt";
		String topKChild4StnLDALMFile = filePrefix + "topChild4Stn_LDA_LM.txt";

		try {
			PrintWriter pw = new PrintWriter(new File(topKChild4StnLDAFile));

			for (_Doc d : m_trainSet) {
				if (d instanceof _ParentDoc) {
					_ParentDoc pDoc = (_ParentDoc) d;

					pw.println(pDoc.getName() + "\t" + pDoc.getSenetenceSize());

					for (_Stn stnObj : pDoc.getSentences()) {
						HashMap<String, Double> likelihoodMap = rankChild4StnByLikelihood(
								stnObj, pDoc);

						int i = 0;
						pw.print((stnObj.getIndex() + 1) + "\t");

						for (Map.Entry<String, Double> e : sortHashMap4String(
								likelihoodMap, true)) {
							// if(i==topK)
							// break;
							pw.print(e.getKey());
							pw.print(":" + e.getValue());
							pw.print("\t");

							i++;
						}
						pw.println();

					}
				}
			}
			pw.flush();
			pw.close();

			PrintWriter LDALMPW = new PrintWriter(new File(topKChild4StnLDALMFile));

			for (_Doc d : m_trainSet) {
				if (d instanceof _ParentDoc) {
					_ParentDoc pDoc = (_ParentDoc) d;

					LDALMPW.println(pDoc.getName() + "\t" + pDoc.getSenetenceSize());

					for (_Stn stnObj : pDoc.getSentences()) {
						HashMap<String, Double> likelihoodMap = rankChild4StnByCombinatorialLikelihood(
								stnObj, pDoc);

						int i = 0;
						LDALMPW.print((stnObj.getIndex() + 1) + "\t");

						for (Map.Entry<String, Double> e : sortHashMap4String(
								likelihoodMap, true)) {
							// if(i==topK)
							// break;
							LDALMPW.print(e.getKey());
							LDALMPW.print(":" + e.getValue());
							LDALMPW.print("\t");

							i++;
						}
						LDALMPW.println();

					}
				}
			}
			LDALMPW.flush();
			LDALMPW.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	protected void printTopKStn4Child(String filePrefix, int topK) {
		String topKStn4ChildFile = filePrefix + "topStn4Child.txt";
		try {
			PrintWriter pw = new PrintWriter(new File(topKStn4ChildFile));

			for (_Doc d : m_trainSet) {
				if (d instanceof _ParentDoc) {
					_ParentDoc pDoc = (_ParentDoc) d;

					pw.println(pDoc.getName() + "\t" + pDoc.m_childDocs.size());

					for (_ChildDoc cDoc : pDoc.m_childDocs) {
						HashMap<Integer, Double> stnSimMap = rankStn4ChildBySim(
								pDoc, cDoc);
						int i = 0;

						pw.print(cDoc.getName() + "\t");
						for (Map.Entry<Integer, Double> e : sortHashMap4Integer(
								stnSimMap, true)) {
							// if(i==topK)
							// break;
							pw.print(e.getKey());
							pw.print(":" + e.getValue());
							pw.print("\t");

							i++;
						}
						pw.println();
					}
				}
			}
			pw.flush();
			pw.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void separateTrainTest4Dynamic() {

		int cvFold = 10;
		ArrayList<String> parentFakeList = new ArrayList<String>();
		String parentFakeString = "37 198 90 84 358 468 381 361 452 164 323 386 276 285 277 206 402 293 354 62 451 161 287 232 337 471 143 93 217 263 260 175 79 237 95 387 391 193 470 196 190 43 135 458 244 464 266 25 303 211";
		// String parentFakeString =
		// "448 348 294 329 317 212 327 127 262 148 307 139 40 325 224 234 233 430 357 78 191 150 424 206 125 484 293 73 456 111 141 68 106 183 215 402 209 159 34 156 280 265 458 65 32 118 352 105 404 66";
		String[] parentFakeStringArray = parentFakeString.split(" ");

		for (String parentName : parentFakeStringArray) {
			parentFakeList.add(parentName);
			System.out.println("parent Name\t" + parentName);
		}

		ArrayList<_Doc> parentTrainSet = new ArrayList<_Doc>();
		double avgCommentNum = 0;
		m_trainSet = new ArrayList<_Doc>();
		m_testSet = new ArrayList<_Doc>();
		for (_Doc d : m_corpus.getCollection()) {
			if (d instanceof _ParentDoc) {
				String parentName = d.getName();
				if (parentFakeList.contains(parentName)) {
					m_testSet.add(d);
					avgCommentNum += ((_ParentDoc) d).m_childDocs.size();
				} else {
					parentTrainSet.add(d);
				}
			}
		}

		System.out.println("avg comments for parent doc in testSet\t"
				+ avgCommentNum * 1.0 / m_testSet.size());

		for (_Doc d : parentTrainSet) {
			_ParentDoc pDoc = (_ParentDoc) d;
			m_trainSet.add(d);
			pDoc.m_childDocs4Dynamic = new ArrayList<_ChildDoc>();
			for (_ChildDoc cDoc : pDoc.m_childDocs) {
				m_trainSet.add(cDoc);
				pDoc.addChildDoc4Dynamics(cDoc);
			}
		}
		System.out.println("m_testSet size\t" + m_testSet.size());
		System.out.println("m_trainSet size\t" + m_trainSet.size());
	}

	public void inferenceTest4Dynamical(int commentNum) {
		m_collectCorpusStats = false;

		for (_Doc d : m_testSet) {
			inferenceDoc4Dynamical(d, commentNum);
		}
	}

	public void printTestParameter4Dynamic(int commentNum) {
		String xProportionFile = "./data/results/dynamic/testChildXProportion_"
				+ commentNum + ".txt";

		String parentParameterFile = "./data/results/dynamic/testParentParameter_"
				+ commentNum + ".txt";
		String childParameterFile = "./data/results/dynamic/testChildParameter_"
				+ commentNum + ".txt";

		printParameter(parentParameterFile, childParameterFile, m_testSet);
	}

	public void inferenceDoc4Dynamical(_Doc d, int commentNum) {
		ArrayList<_Doc> sampleTestSet = new ArrayList<_Doc>();
		initTest4Dynamical(sampleTestSet, d, commentNum);

		double tempLikelihood = inference4Doc(sampleTestSet);
	}

	// dynamical add comments to sampleTest
	public void initTest4Dynamical(ArrayList<_Doc> sampleTestSet, _Doc d,
			int commentNum) {
		_ParentDoc pDoc = (_ParentDoc) d;
		pDoc.m_childDocs4Dynamic = new ArrayList<_ChildDoc>();
		pDoc.setTopics4Gibbs(number_of_topics, d_alpha);
		for (_Stn stnObj : pDoc.getSentences()) {
			stnObj.setTopicsVct(number_of_topics);
		}

		sampleTestSet.add(pDoc);
		int count = 0;
		for (_ChildDoc cDoc : pDoc.m_childDocs) {
			if (count >= commentNum) {
				break;
			}
			count++;
			cDoc.setTopics4Gibbs_LDA(number_of_topics, d_alpha);
			sampleTestSet.add(cDoc);
			pDoc.addChildDoc4Dynamics(cDoc);
		}
	}

	public void mixTest4Spam() {
		int t = 0, j1 = 0;
		_ChildDoc tmpDoc1;
		int testSize = m_testSet.size();
		for (int i = 0; i < testSize; i++) {
			t = m_rand.nextInt(testSize);

			while (t == i)
				t = m_rand.nextInt(testSize);

			_ParentDoc pDoc1 = (_ParentDoc) m_testSet.get(i);

			_ParentDoc pDoc2 = (_ParentDoc) m_testSet.get(t);
			int pDocCDocSize2 = pDoc2.m_childDocs.size();

			j1 = m_rand.nextInt(pDocCDocSize2);
			tmpDoc1 = (_ChildDoc) pDoc2.m_childDocs.get(j1);

			pDoc1.addChildDoc(tmpDoc1);

		}
	}

	public void separateTrainTest4Spam() {
		int cvFold = 10;
		ArrayList<String> parentFakeList = new ArrayList<String>();
		String parentFakeString = "448 348 294 329 317 212 327 127 262 148 307 139 40 325 224 234 233 430 357 78 191 150 424 206 125 484 293 73 456 111 141 68 106 183 215 402 209 159 34 156 280 265 458 65 32 118 352 105 404 66";
		String[] parentFakeStringArray = parentFakeString.split(" ");

		for (String parentName : parentFakeStringArray) {
			parentFakeList.add(parentName);
			System.out.println("parent Name\t" + parentName);
		}

		ArrayList<_Doc> parentTrainSet = new ArrayList<_Doc>();
		double avgCommentNum = 0;
		m_trainSet = new ArrayList<_Doc>();
		m_testSet = new ArrayList<_Doc>();
		for (_Doc d : m_corpus.getCollection()) {
			if (d instanceof _ParentDoc) {
				String parentName = d.getName();
				if (parentFakeList.contains(parentName)) {
					m_testSet.add(d);
					avgCommentNum += ((_ParentDoc) d).m_childDocs.size();
				} else {
					parentTrainSet.add(d);
				}
			}
		}

		System.out.println("avg comments for parent doc in testSet\t"
				+ avgCommentNum * 1.0 / m_testSet.size());

		for (_Doc d : parentTrainSet) {
			_ParentDoc pDoc = (_ParentDoc) d;
			m_trainSet.add(d);
			for (_ChildDoc cDoc : pDoc.m_childDocs) {
				m_trainSet.add(cDoc);
			}
		}
		System.out.println("m_testSet size\t" + m_testSet.size());
		System.out.println("m_trainSet size\t" + m_trainSet.size());
	}

	public void inferenceTest4Spam() {
		m_collectCorpusStats = false;

		for (_Doc d : m_testSet) {
			inferenceDoc4Spam(d);
		}
	}

	public void inferenceDoc4Spam(_Doc d) {
		ArrayList<_Doc> sampleTestSet = new ArrayList<_Doc>();
		initTest4Spam(sampleTestSet, d);
		double tempLikelihood = inference4Doc(sampleTestSet);
	}

	public void initTest4Spam(ArrayList<_Doc> sampleTestSet, _Doc d) {
		_ParentDoc pDoc = (_ParentDoc) d;
		pDoc.setTopics4Gibbs(number_of_topics, 0);
		for (_Stn stnObj : pDoc.getSentences()) {
			stnObj.setTopicsVct(number_of_topics);
		}
		sampleTestSet.add(pDoc);

		for (_ChildDoc cDoc : pDoc.m_childDocs) {
			cDoc.setTopics4Gibbs_LDA(number_of_topics, d_alpha);
			sampleTestSet.add(cDoc);
			cDoc.setParentDoc(pDoc);
		}
	}

	public void printTestParameter4Spam(String filePrefix) {
		String parentParameterFile = filePrefix + "testParentParameter.txt";
		String childParameterFile = filePrefix + "testChildParameter.txt";

		printParameter(parentParameterFile, childParameterFile, m_testSet);
	}

	protected int rankStn4ParentByMajorTopic(_Stn stnObj,
											 _ParentDoc4DCM pDoc) {

		int[] topicNumArray = new int[number_of_topics];
		Arrays.fill(topicNumArray, 0);
		int maxTopicIndex = 0;
		double maxTopicRatio = 0;
		for (_Word w : stnObj.getWords()) {
			int wid = w.getIndex();
			int sparseWid = pDoc.m_word2Index.get(wid);
			for(int k=0; k<number_of_topics; k++) {
				topicNumArray[k] += pDoc.m_phi[sparseWid][k];
				if(maxTopicRatio<topicNumArray[k]) {
					maxTopicIndex = k;
					maxTopicRatio = topicNumArray[k];
				}
			}
		}

		return maxTopicIndex;

	}

	protected void estimateTopicProb4Words(_ParentDoc4DCM pDoc){
		int uniqueWordsNum = pDoc.getSparse().length;
		for(int i=0; i<uniqueWordsNum; i++){
			Arrays.fill(pDoc.m_phi[i], 0);
		}

		for(_Word w: pDoc.getWords()){
			int tid = w.getTopic();
			int wid = w.getIndex();
			int sparseWid = pDoc.m_word2Index.get(wid);
			pDoc.m_phi[sparseWid][tid] ++;
		}

		for(int i=0; i<uniqueWordsNum; i++){
			double phiSum = 0;
			phiSum = Utils.sumOfArray(pDoc.m_phi[i]);
			for(int k=0; k<number_of_topics; k++){
				pDoc.m_phi[i][k] /= phiSum;
			}
		}
	}

	protected double rankStn4ParentByParentLikelihood(_Stn stnObj,
													  _ParentDoc4DCM pDoc) {

		double parentLikelihood = 0;

		double[] topicProportion = new double[number_of_topics];
		Arrays.fill(topicProportion, 0);
		for (_Word w : stnObj.getWords()) {
			int wid = w.getIndex();
			int sparseWid = pDoc.m_word2Index.get(wid);
			for(int k=0; k<number_of_topics; k++) {
				topicProportion[k] += pDoc.m_phi[sparseWid][k];
			}
		}

		Utils.L1Normalization(topicProportion);

		for(_Word w:pDoc.getWords()){
			int wid = w.getIndex();

			double wordLikelihood = 0;
			for(int k=0; k<number_of_topics; k++){
				wordLikelihood += topicProportion[k]*topic_term_probabilty[k][wid];
			}
			parentLikelihood += Math.log(wordLikelihood);
		}

		return parentLikelihood;
	}

	protected double rankStn4ParentByNormLikelihood(_Stn stnObj,
													_ParentDoc4DCM pDoc) {
		double stnLogLikelihood = 0;
		for (_Word w : stnObj.getWords()) {
			double wordLikelihood = 0;
			int wid = w.getIndex();

			for (int k = 0; k < number_of_topics; k++) {
				wordLikelihood += pDoc.m_topics[k] *topic_term_probabilty[k][wid];
			}

			stnLogLikelihood += Math.log(wordLikelihood);
		}

		return stnLogLikelihood;
	}

}
