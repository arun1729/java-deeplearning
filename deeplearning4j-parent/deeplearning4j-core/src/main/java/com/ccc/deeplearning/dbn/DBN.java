package com.ccc.deeplearning.dbn;

import org.apache.commons.math3.random.RandomGenerator;
import org.jblas.DoubleMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ccc.deeplearning.nn.BaseMultiLayerNetwork;
import com.ccc.deeplearning.nn.NeuralNetwork;
import com.ccc.deeplearning.rbm.RBM;

/**
 * Deep Belief Network. This is a MultiLayer Perceptron Model
 * using Restricted Boltzmann Machines.
 *  See Hinton's practical guide to RBMs for great examples on
 *  how to train and tune parameters.
 *  
 * @author Adam Gibson
 *
 */
public class DBN extends BaseMultiLayerNetwork {

	private static final long serialVersionUID = -9068772752220902983L;
	private static Logger log = LoggerFactory.getLogger(DBN.class);

	public DBN() {}


	public DBN(int n_ins, int[] hidden_layer_sizes, int n_outs, int n_layers,
			RandomGenerator rng, DoubleMatrix input,DoubleMatrix labels) {
		super(n_ins, hidden_layer_sizes, n_outs, n_layers, rng, input,labels);
	}



	public DBN(int n_ins, int[] hidden_layer_sizes, int n_outs, int n_layers,
			RandomGenerator rng) {
		super(n_ins, hidden_layer_sizes, n_outs, n_layers, rng);
	}


	/**
	 * 
	 * @param input input examples
	 * @param labels output labels
	 * @param otherParams
	 * 
	 * (int)    k
	 * (double) learningRate
	 * (int) epochs
	 * 
	 * Optional:
	 * (double) finetune lr
	 * (int) finetune epochs
	 * 
	 */
	@Override
	public void trainNetwork(DoubleMatrix input, DoubleMatrix labels,
			Object[] otherParams) {
		int k = (Integer) otherParams[0];
		double lr = (Double) otherParams[1];
		int epochs = (Integer) otherParams[2];
		pretrain(input,k,lr,epochs);
		if(otherParams.length < 3)
			finetune(labels, lr, epochs);
		else {
			double finetuneLr = otherParams.length > 3 ? (double) otherParams[3] : lr;
			int finetuneEpochs = otherParams.length > 4 ? (int) otherParams[4] : epochs;
			finetune(labels,finetuneLr,finetuneEpochs);
		}
	}

	/**
	 * This unsupervised learning method runs
	 * contrastive divergence on each RBM layer in the network.
	 * @param input the input to train on
	 * @param k the k to use for running the RBM contrastive divergence.
	 * The typical tip is that the higher k is the closer to the model
	 * you will be approximating due to more sampling. K = 1
	 * usually gives very good results and is the default in quite a few situations.
	 * @param learningRate the learning rate to use
	 * @param epochs the number of epochs to train
	 */
	public void pretrain(DoubleMatrix input,int k,double learningRate,int epochs) {

		if(this.input == null) {
			this.input = input;
			initializeLayers(input);
		}
		else 
			this.input = input;

		DoubleMatrix layerInput = null;

		for(int i = 0; i < nLayers; i++) {
			if(i == 0)
				layerInput = this.input;
			else 
				layerInput = sigmoidLayers[i-1].sampleHGivenV(layerInput);
			log.info("Training on layer " + (i + 1));
			
			Integer numTimesOver = 1;
			Double bestLoss = Double.POSITIVE_INFINITY;
			
			DoubleMatrix bestWeights = layers[i].getW();
			DoubleMatrix bestHBias = layers[i].gethBias();
			DoubleMatrix bestVbias = layers[i].getvBias();
			
			for(int  epoch = 0; epoch < epochs; epoch++)  {
				boolean trainedProperly = this.trainNetwork(layers[i],sigmoidLayers[i], epoch, i, layerInput, learningRate,bestLoss,new Object[]{k});
				
				if(!trainedProperly)
					numTimesOver++;
				else {
					bestLoss = layers[i].getReConstructionCrossEntropy();
					bestWeights = layers[i].getW();
					bestHBias = layers[i].gethBias();
					bestVbias = layers[i].getvBias();
					
				}
				
				if(numTimesOver >= 30) {
					log.info("Breaking early; " + numTimesOver);
					layers[i].setW(bestWeights);
					layers[i].sethBias(bestHBias);
					layers[i].setvBias(bestVbias);
					break;
				}
			}

		}
	}

	public void pretrain(int k,double learningRate,int epochs) {
		pretrain(this.input,k,learningRate,epochs);
	}


	@Override
	public NeuralNetwork createLayer(DoubleMatrix input, int nVisible,
			int nHidden, DoubleMatrix W, DoubleMatrix hBias,
			DoubleMatrix vBias, RandomGenerator rng,int index) {
		RBM ret = new RBM.Builder().useRegularization(useRegularization)
		.numberOfVisible(nVisible).numHidden(nHidden).withWeights(W)
		.withInput(input).withVisibleBias(vBias).withHBias(hBias)
		.withRandom(rng).renderWeights(renderWeightsEveryNEpochs)
		.fanIn(fanIn).build();
		return ret;
	}

	@Override
	public NeuralNetwork[] createNetworkLayers(int numLayers) {
		return new RBM[numLayers];
	}


	public static class Builder extends BaseMultiLayerNetwork.Builder<DBN> {
		public Builder() {
			this.clazz = DBN.class;
		}
	}




}