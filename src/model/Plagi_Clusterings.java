package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class Plagi_Clusterings {
	HashMap<String, StudentPair> interestingPairs;
	double rsThreshold; // relative similarity threshold, used to determine whether a comparison goes into interestingPairs
	double csThreshold; // combination/clustering simlarity threshold, used to determine whether or not we merge two sets of students
	static ConcurrentHashMap<Integer, Set<Student>> groupings; //will start off as the set version of all interesting pairs, and then we condense as much as we can. This will then contain the desired groups/clusterings
	
	public Plagi_Clusterings(double rsThreshold, double csThreshold){
		this.rsThreshold = rsThreshold;
		this.csThreshold = csThreshold;
		this.interestingPairs = new HashMap<String, StudentPair>();
		this.groupings = new ConcurrentHashMap<Integer,Set<Student>>();
	}
	
	public void addInterestingPair(StudentPair p){ // to be used by various clusterers
		if(p.relativeScore >= rsThreshold)interestingPairs.put(p.id, p);
	}
	
	public void loadInterestingPairs(){ //assuming we have now populated interestingPairs, we load them all into our groupings, initializing our clustering
		int count = 0;
		for (StudentPair p : interestingPairs.values()) {
			HashSet<Student> toAdd = new HashSet<Student>();
			toAdd.add(p.left);
			toAdd.add(p.right);
			groupings.put(count,toAdd);
			count++;
		}
	}
	
	public void cluster(){ //assuming we have an initialized groupings, we now cluster recursively until no combinations can be made
		boolean combinationsMade = false; //terminating condition
		for (Set<Student> s : groupings.values()) {
			ArrayList<Integer> keysToEvict = new ArrayList<Integer>();
			for (int i : groupings.keySet()) { // s prime
				Set<Student> s_p = groupings.get(i);
				if(s.equals(s_p))continue;
				Set<Student> s_d = new HashSet<Student>(); // XOR of s and s_p
				Set<Student> s_u = new HashSet<Student>(); // Union of s and s_p
				Set<Student> s_i = new HashSet<Student>(); // Intersection of s and s_p
				s_u.addAll(s); 
				s_u.addAll(s_p); // s_u init
				s_i.addAll(s); 
				s_i.retainAll(s_p); // s_i init
				s_d.addAll(s_u); 
				s_d.removeAll(s_i); //s_d init
				ArrayList<String> keys = deriveKeys(s_d); //will generate all keys to look up studentPairs
				boolean doNothing = false; //will set to true if we see that s and s_p should not be combined
				for (String k : keys) { //note that the key may be in the opposite order contained in hashmap
					String k_reversed =  k.substring(k.indexOf('|') + 1)+ "|" +k.split("\\|")[0] ;
					StudentPair pk = interestingPairs.get(k);
					StudentPair pkr = interestingPairs.get(k_reversed);
					if(pk == null && pkr == null){
						doNothing = true;
						break;
					}
					if(pk !=null && pkr!=null)throw new Error("WHAT??");
					if(pk!=null){
						doNothing = pk.relativeScore < csThreshold;
					}
					else{
						doNothing = pkr.relativeScore < csThreshold;
					}
				}
				if(!doNothing){
					s.clear();
					s.addAll(s_u);
					keysToEvict.add(i);
					combinationsMade = true;
				}
			}
			for (Integer k : keysToEvict) groupings.remove(k);
		}
		if(combinationsMade) cluster();

	}
	
	private ArrayList<String> deriveKeys(Set<Student> students) {
		ArrayList<String> toRet = new ArrayList<String>();
		Object[] arr = students.toArray();
		for(int i =0; i < arr.length -1;i++){
			for(int j = i+1; j< arr.length; j++){
				Student i_s = (Student) arr[i];
				Student j_s = (Student) arr[j];
				toRet.add(i_s.id + "|" + j_s.id);
			}
		}
		return toRet;
	}
	
	public static void printClustering(){
		for (Set<Student> s:groupings.values()) {
			System.out.print("{ ");
			for (Student student : s) {
				System.out.print(student.id+" ");
			}
			System.out.println("}");
		}
	}

	// getters
	public HashMap<String, StudentPair> getInterestingPairs() {
		return interestingPairs;
	}
	public double getRsThreshold() {
		return rsThreshold;
	}
	public double getCsThreshold() {
		return csThreshold;
	}
	public ConcurrentHashMap<Integer,Set<Student>> getGroupings() {
		return groupings;
	}

	public double computeAverageSize() {
		int count = 0;
		int total = 0;
		for (Set<Student> set : groupings.values()) {
			if(set.size()==0)continue;
			count++;
			total += set.size();
		}
		return total/count;
	}

	public int[] computeHistogram() {
		int maxSize = 0;
		for (Set<Student> set : groupings.values()) {
			if(set.size()>maxSize)maxSize=set.size();
		}
		int[]h = new int[maxSize];
		for(int i = 1; i<= maxSize;i++){
			int count = 0;
			for (Set<Student> set : groupings.values()) {
				if(set.size()==i)count++;
			}
			h[i-1]=count;
		}
		return h;
	}
}
