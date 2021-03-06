package DeepRLPackage;


import org.nd4j.linalg.api.ndarray.INDArray;

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

/**
 *
 * @author Anton
 */
public class SarsSample {
    private INDArray _state;
    private String _action;
    private double _reward;
    private INDArray _nextState;
    private boolean isTerminal;
    
    public SarsSample(INDArray state,String action,double reward,INDArray nextState,boolean isTerminal){
        _state = state;
        _action = action;
        _reward = reward;
        _nextState = nextState;
        isTerminal = isTerminal;
    }
    
    public INDArray getState(){
        return _state;
    }
    
    public boolean isTerminal(){
        return isTerminal;
    }
    
    public String getAction(){
        return _action;
    }
    
    public double getReward(){
        return _reward;
    }
    
    public INDArray getNextState(){
        return _nextState;
    }
}
