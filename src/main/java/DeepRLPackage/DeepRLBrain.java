package DeepRLPackage;

/*
 * Copyright (C) 2017 AMIS research group, Faculty of Mathematics and Physics, Charles University in Prague, Czech Republic
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.nd4j.linalg.factory.Nd4j;

/**
 *
 * @author Anton
 */
public class DeepRLBrain {    
    private static final int seed = 123;
    private static final double learningRate = 0.01;
    private static final int numHiddenNodes = 25;
    public double epsilon;
    private static final double gamma = 0.9;
    private static final int replayMemorySize = 10000;
    private static final int epochLength = 5000;
    private static final int minibatchSize = 25;
    private static final boolean SHOULD_LOAD = true;
    private File saveFile;    
    private int indexOfReplayMemory = 0,nrOfUpdatePasses = 0;
    MultiLayerNetwork model,sampleMakerModel;
    private ArrayList<SarsSample> replayMemory;
    
    private int nrPolicyUsed = 0;
    private int nrOfFeatures;
    private int nrOfActions;
    private double mse = 0;
    private double meanReward = 0;
    private String folderPath;
    private ArrayList<String> policyValuesOfState;
    public boolean hasSaved = false;
    
    public DeepRLBrain(File file, double epsilonValue,int numInputs,int numOutputs,String path){
        folderPath = path;
        nrOfFeatures = numInputs;
        nrOfActions = numOutputs;
        policyValuesOfState = new ArrayList<String>();
        epsilon = epsilonValue;
        System.out.println("Epsilon = "+epsilonValue);
        saveFile = file;
        
        if(!SHOULD_LOAD || saveFile == null){
            MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(seed)
                .iterations(1)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .learningRate(learningRate)
                .updater(org.deeplearning4j.nn.conf.Updater.NESTEROVS)
                    .momentum(org.nd4j.linalg.learning.Nesterovs.DEFAULT_NESTEROV_MOMENTUM)
                .list()
                .layer(0,new DenseLayer.Builder()
                       .nIn(numInputs)
                       .nOut(numHiddenNodes)
                       .weightInit(WeightInit.RELU_UNIFORM)
                        .biasInit(0.1)
                       .activation(Activation.RELU)
                       .build())
                    .layer(1,new DenseLayer.Builder()
                       .nIn(numHiddenNodes)
                       .nOut(numHiddenNodes)
                       .weightInit(WeightInit.RELU_UNIFORM)
                        .biasInit(0.1)
                       .activation(Activation.RELU)
                       .build())
                    .layer(2,new DenseLayer.Builder()
                       .nIn(numHiddenNodes)
                       .nOut(numHiddenNodes)
                       .weightInit(WeightInit.RELU_UNIFORM)
                        .biasInit(0.1)
                       .activation(Activation.RELU)
                       .build())
                .layer(3,new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                       .weightInit(WeightInit.XAVIER)
                       .activation(Activation.IDENTITY)
                       .nIn(numHiddenNodes)
                       .nOut(numOutputs)
                       .build()
                       )
                .pretrain(false).backprop(true).build();
            model = new MultiLayerNetwork(conf);
            sampleMakerModel = new MultiLayerNetwork(conf);
            sampleMakerModel.init();
            model.init();
        }
        else{
            try{
                Nd4j.getRandom().setSeed(seed);                
                model = ModelSerializer.restoreMultiLayerNetwork(saveFile,true);
                sampleMakerModel = ModelSerializer.restoreMultiLayerNetwork(saveFile,true);
                model.init();
                sampleMakerModel.init();
                System.out.println("Loaded! " + saveFile.getName());
            }catch(Exception e){
                System.out.println("ERROR LOADING NETWORK!!!!!!!!!!!!!!!!!");
            }
        }
        replayMemory = new ArrayList<SarsSample>();        
    }
    
    public String computeActionFromQValues(IGameState state,List<String> posActions,boolean isTraining){           
        INDArray q = sampleMakerModel.output(state.getState(),isTraining);
        int actionIndex = (int)Nd4j.argMax(q).getDouble(0,0); 
        
        return posActions.get(actionIndex); 
    }
    public double computeValueFromQValues(IGameState state, List<String> posActions){        
        INDArray q = sampleMakerModel.output(state.getState(),true);  
        double value = (int)Nd4j.max(q).getDouble(0,0);
        
        return value;
    }
    
    public double getQValue(IGameState state,String action,List<String> posActions){
        INDArray temp = sampleMakerModel.output(state.getState(),false);
        double a = temp.getDouble(posActions.indexOf(action));
        return a;        
    }
    public double getValue(IGameState state, List<String> posActions){
        return computeValueFromQValues(state,posActions);
    }
    
    public String getAction(IGameState state, List<String> posActions){        
        Random random = new Random();        
        //With a probability of epsilon, return a random action
        if(random.nextDouble() < epsilon){
            int temp = ThreadLocalRandom.current().nextInt(0,posActions.size());            
            
            policyValuesOfState.add(sampleMakerModel.output(state.getState(),true).toString());            
            
            return posActions.get(temp);   
        }
        else{
            nrPolicyUsed++;
            policyValuesOfState.add(sampleMakerModel.output(state.getState(),true).toString());
            return computeActionFromQValues(state,posActions,true);
        }
    }
    public String getPolicy(IGameState state, List<String> posActions){
        return computeActionFromQValues(state, posActions,false);      
    }    
    
    
    
    public void Update(IGameState state, String action, IGameState nextState, double currentTransitionReward, List<String> posActions, Boolean isTerminalState){
        double policyEstimatedValueOfAction;
        int transitionActionIndex;
        
        //<editor-fold defaultstate="collapsed" desc="Add sarsSample to replayMemory">
        if(replayMemory.size() != replayMemorySize)
            replayMemory.add(new SarsSample(state.getState(), action, currentTransitionReward, nextState.getState(),isTerminalState));
        else{
            replayMemory.set(indexOfReplayMemory, new SarsSample(
                    state.getState(), action, currentTransitionReward, nextState.getState(),isTerminalState));
            indexOfReplayMemory+=1;
            if(indexOfReplayMemory == replayMemorySize)
                indexOfReplayMemory = 0;
        }
//</editor-fold>
        
        //<editor-fold defaultstate="collapsed" desc="Create miniBatch">
        SarsSample[] miniBatch;
        if(replayMemory.size() <= minibatchSize){ // If replay memory smaller than minibatch then
            miniBatch = new SarsSample[replayMemory.size()];
        }
        else{
            miniBatch = new SarsSample[minibatchSize];
        }
        for(int i = 0; i < miniBatch.length; i++){
            miniBatch[i] = replayMemory.get(
                    ThreadLocalRandom.current().nextInt(0,replayMemory.size()));
        }
        
//</editor-fold>    

        //<editor-fold defaultstate="collapsed" desc=" Get statesToTrainOn and transitionRewardsToTrainOn to update the model. Calculate MSE and MR">
        INDArray transitionRewardsToTrainOn = Nd4j.create(new int[]{miniBatch.length,nrOfActions}); 
        INDArray statesToTrainOn = Nd4j.create(new int[]{miniBatch.length,nrOfFeatures});        
        
        //UPDATE THE TRANSITION REWARDS IN THE MINIBATCH WITH NEW ESTIMATIONS BASED ON NEW Q1
        for (int i = 0; i < miniBatch.length;i++) {
            INDArray q = sampleMakerModel.output(miniBatch[i].getState(), true); // Get policy for state
            INDArray q1 = sampleMakerModel.output(miniBatch[i].getNextState(), true); //Get policy for nextState
            transitionActionIndex = posActions.indexOf(miniBatch[i].getAction()); //Index of Action used in the Transition
            policyEstimatedValueOfAction = q.getDouble(transitionActionIndex);   // Estimated reward for action, ONLY USED FOR MSE!
            statesToTrainOn.putRow(i, miniBatch[i].getState()); //Add state to the trainingList
            
            //CALCULATE NEW TRANSITION REWARD//
            double reward = miniBatch[i].getReward(); // Get the reward returned from the game for transition from state to nextState
            double rewardOptimalTransitionInNextState = (Nd4j.max(q1).getDouble(0,0)); // Get Reward for optimal Action in nextState
            double actualRewardOfActionInState = reward + gamma * rewardOptimalTransitionInNextState; //Calculate reward recieved for transition from state to nextState and playing the game optimally from there
            
            if(miniBatch[i].isTerminal())
                actualRewardOfActionInState = reward;
            
            q.putScalar(0,transitionActionIndex, actualRewardOfActionInState); // Put the actual reward for the action into the policy for state
            transitionRewardsToTrainOn.putRow(i,q); //Save the updated policy to be used for training
            
            /////////////////
            //CALCULATE MSE//
            mse += Math.abs(Math.abs(policyEstimatedValueOfAction) - Math.abs(transitionRewardsToTrainOn.getRow(i).getDouble(transitionActionIndex))); //Prediction - actual_value_of_action
            meanReward += reward;
        }
//</editor-fold>
        model.fit(statesToTrainOn,transitionRewardsToTrainOn);
        
        //<editor-fold defaultstate="collapsed" desc="Save File?">
        nrOfUpdatePasses+=1;
        if(nrOfUpdatePasses == epochLength){
            hasSaved = true;
            //Save the model            
            String id = java.util.Calendar.getInstance().getTime().toString().trim().replaceAll(" ", "-");
            id = id.trim().replace(":", "_");
            File locationToSave = new File(folderPath + id +".zip");
            boolean saveUpdater = true;            
            try{
                ModelSerializer.writeModel(model, locationToSave, saveUpdater);
            }catch(Exception e){
                System.out.println("EXCEPTION SAVING NETWORK!!!!!!!!!!!!!!!!!"+ e.toString());
            }
            
            //Print Estimations
            for(Iterator<String> i = policyValuesOfState.iterator(); i.hasNext();){
                System.out.println(i.next());
            }
            
            policyValuesOfState = new ArrayList<String>();
            System.out.println("*******************************************");
            System.out.println("*******************************************"); 
            System.out.println("--------------------------");
            System.out.println("MSE: "+mse / epochLength);
            System.out.println("MR: "+meanReward / epochLength);
            System.out.println("Over: "+epochLength+" updates");
            System.out.println("Policy Used: "+nrPolicyUsed+" updates");
            System.out.println("--------------------------");
            System.out.println("---------SAVED! "+id);            
            System.out.println("*******************************************");
            System.out.println("*******************************************");
        }
        //</editor-fold>

    }    
}
