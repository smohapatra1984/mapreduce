package mapreduce;

import java.util.*;

public class MapReduce<InputMapKey, InputMapValue, IntermediateKey extends Comparable<IntermediateKey >, IntermediateValue, OutputReduceKey, OutputReduceValue>{

	
	Class<? extends Mapper<InputMapKey, InputMapValue, IntermediateKey, IntermediateValue>> mapClass;
	Class<? extends Reducer< IntermediateKey, IntermediateValue, OutputReduceKey, OutputReduceValue>> reduceClass;
	
//	Mapper<Key, Value> mapper;
//	Reducer<Key, Value> reducer;
	InputData<InputMapKey, InputMapValue, IntermediateKey, IntermediateValue> inputData;
	OutputData<OutputReduceKey, OutputReduceValue> outputData;
	
	/*
	 * phase_mp�ϐ��łǂ̃t�F�[�Y�܂Ŏ��s���邩�����ɂ߂�
	 * phase_mp��"MAP_ONLY"�Ȃ��Map�����̂�
	 * "MAP_SHUFFLE"�Ȃ��Map��Shuffle����
	 * "MAP_REDUCE"0�ȏ�Ȃ��Map��Shuffle��Reduce�̎O���s��
	 * ����ȊO�̒l�ł����"MAP_REDUCE"�Ƃ��Ĉ���
	 */
	
	String phaseMR;
	
	/*
	 * result_output�̓t�F�[�Y���Ƃ̌��ʂ̏o�͂�on/off�ɂ��Ē�`����
	 * 
	 * 
	 */
	boolean resultOutput;

	
	public MapReduce(Class<? extends Mapper<InputMapKey, InputMapValue, IntermediateKey, IntermediateValue>> map_class, Class<? extends  Reducer< IntermediateKey, IntermediateValue, OutputReduceKey, OutputReduceValue>> reduce_class, String phase_mp){
		this.mapClass = map_class;
		this.reduceClass = reduce_class;
		this.inputData = new InputData<InputMapKey, InputMapValue, IntermediateKey, IntermediateValue>();
		this.outputData = new OutputData<OutputReduceKey, OutputReduceValue>();
		this.phaseMR = phase_mp;
		this.resultOutput = true;
	}
	
	
	/*
	 * addKeyValue
	 * pass data formated as key-value pairs to inputData
	 */
	
	public void addKeyValue(InputMapKey key, InputMapValue value){
		this.inputData.putKeyValue(key, value);
	}
	
	/*
	 * Map�֐������s����
	 * �eKey-Value�ɑ΂��ȉ��̏������s��
	 * 1.Mapper�C���X�^���X�̐���
	 * 2.Mapper�C���X�^���X��
	 * 
	 */
	void startMap(){
		Mapper<InputMapKey, InputMapValue, IntermediateKey, IntermediateValue> local_map;
		ArrayList<IntermediateKey> okeys;
		ArrayList<IntermediateValue> ovalues;
		
		for(int i = 0; i < this.inputData.getMapSize(); i ++){
			try{
				local_map = mapClass.newInstance();
			}catch (InstantiationException e) {
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
			
			local_map.setKeyValue(this.inputData.getMapKey(i), this.inputData.getMapValue(i));
			
			local_map.map();

			//Map�֐��̏o�͂�����
			okeys = local_map.getKeys();
			ovalues = local_map.getValues();
			for(int j = 0; j < okeys.size(); j++){
				this.inputData.setMap(okeys.get(j), ovalues.get(j));
			}
			
			//�������̈�̉��
			okeys = null ;
			ovalues = null;
			local_map = null;
		}
		
		//Map������S�ďI������input_data���̏����l��ۑ����Ă��郁�����̈�����
		this.inputData.initialRelease();
	}
	
	void startShuffle(){
		this.inputData.qSort(0, this.inputData.getShuffleSize()-1);
		this.inputData.grouping();
	}
	
	void startReduce(){
		Reducer<IntermediateKey, IntermediateValue,OutputReduceKey, OutputReduceValue> local_reduce;
		for(int i = 0; i < this.inputData.getReduceSize(); i ++){
			try{
				local_reduce = reduceClass.newInstance();
			}catch (InstantiationException e) {
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
			
			local_reduce.setKeyValue(this.inputData.getReduceKey(i), this.inputData.getReduceValues(i));
			local_reduce.reduce();
			this.outputData.set(local_reduce.getKey(), local_reduce.getValue());
		}
		//�������̈���
		inputData = null;
	}
	
	public void run(){
		startMap();
		if(this.phaseMR.equals("MAP_ONLY")){
			if(this.resultOutput)
				inputData.showMap();
			return;
		}
		startShuffle();
		if(this.phaseMR.equals("MAP_SHUFFLE")){
			if(this.resultOutput)
				inputData.showSuffle();
			return;
		}
		startReduce();
		if(this.resultOutput)
			outputData.reduce_show();
	}
	
	
}
