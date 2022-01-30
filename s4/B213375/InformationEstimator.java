package s4.B213375;
import java.lang.*;
import java.util.Arrays;
import s4.specification.*;

/* Used content imported from s4.specification:
package s4.specification;
public interface InformationEstimatorInterface{
    void setTarget(byte target[]);  // set the data for computing the information quantities
    void setSpace(byte space[]);  // set data for sample space to computer probability
    double estimation();  // It returns 0.0 when the target is not set or Target's length is zero;
        // It returns Double.MAX_VALUE, when the true value is infinite, or space is not set.
        // The behavior is undefined, if the true value is finite but larger than Double.MAX_VALUE.
        // Note that this happens only when the space is unreasonably large. We will encounter other problem anyway.
        // Otherwise, estimation of information quantity.
}
*/

public class InformationEstimator implements InformationEstimatorInterface{
    static boolean debugMode = false;
    static boolean fullCalculations = false; //prevents skipping of some unnecessary calculations
    // Code to test, *warning: This code is slow, and it lacks the required test
    byte[] myTarget; // data to compute its information quantity
    byte[] mySpace;  // Sample space to compute the probability
    FrequencerInterface myFrequencer;  // Object for counting frequency

    private void showVariables(){
        System.out.print("spc:");
        for(int i=0; i<mySpace.length; i++) System.out.write(mySpace[i]);
        System.out.write(' ');

        System.out.print("tgt:");
        for(int i=0; i<myTarget.length; i++) System.out.write(myTarget[i]);
        System.out.write(' ');
    }

    byte[] subBytes(byte[] x, int start, int end){
        // similar to String.substring(start,end)
        byte[] result=new byte[end-start];
        for(int i=0; i<end-start; i++) result[i]=x[start+i];
        return result;
    }

    // IQ: Information Quantity for a count := -log2(count/|space|)
    double iq(int freq){return -Math.log10((double)freq/mySpace.length)/Math.log10(2.0);}

    @Override
    public void setTarget(byte[] target){myFrequencer.setTarget((myTarget=target));}
    @Override
    public void setSpace(byte[] space){
        myFrequencer = new Frequencer();
        myFrequencer.setSpace((mySpace=space));
    }

    @Override
    public double estimation(){//estimation of IQ
        // 0.0 : when no Target;
        // Double.MAX_VALUE : when space not set (technically infinite)
        // WARN: Undefined behaviour if true value is finite but larger than Double.MAX_VALUE.
        if(myTarget.length==0) return 0;
        if(mySpace.length==0) return Double.MAX_VALUE;

        /*  This implementation breaks down the problem into multiple calculations,
            out of which the minimum value out of the candidates is the true result.
            The candidates are each a {
                summation of {
                    the Information Quantity of every substring from {
                        a partition of the original Space
                    } with the same original Target
                }
            }, considering all the possible permutations to subdivide Space.*/
        double output = Double.MAX_VALUE; // init @ worst case, then reduce if possible
        boolean[] partition = new boolean[myTarget.length+1]; // used to define a permutation
        int np = 1<<(myTarget.length-1); // number of ways to partition myTarget : 2^(n-1)

        if(debugMode)showVariables();
        if(debugMode)System.out.printf("np=%d length=%d\n", np, +myTarget.length);

        // subestimations[start,end] := IQ(myTarget.substring(start,end) in mySpace), computed as needed and cached
        double[][] subestimations = new double[myTarget.length+1][myTarget.length+1];
        for(double[] arr:subestimations) Arrays.fill(arr, -1); // init as -1 to signify "not yet calculated"

        for(int p=0; p<np; p++){
            // boolean array canonical form of the p-th partition permutation
            /* e.g. partition permutation {"ab" "cde" "fg"}
                a b c d e f g   : myTarget[]
                T F T F F T F T : partition[] (TRUE iff mT[i] starts a substring)
            */
            partition[0] = true;
            for(int i=0; i<myTarget.length-1; i++)
                partition[i+1] = (0!=((1<<i)&p)); // binary uniquely maps a unique p-th permutation
            partition[myTarget.length] = true;

            // compute Information Quantity for this partition candidate as "value1"
            /* e.g. {"ab" "cde" "fg"} -> value1 := IQ("ab")+IQ("cde")+IQ("fg") */
            double value1 = 0.0;
            int end = 0, start = 0;
            while(start<myTarget.length){
                if(debugMode)System.out.write(myTarget[end]);
                end++;
                while(partition[end]==false){
                    if(debugMode)System.out.write(myTarget[end]);
                    end++;
                }
//                myFrequencer.setTarget(subBytes(myTarget, start, end));
                if(debugMode)System.out.print("["+start+"~"+end+")");
                if(subestimations[start][end]==-1){
                    subestimations[start][end]=this.iq(myFrequencer.subByteFrequency(start, end));
                    if(debugMode)System.out.print("{="+subestimations[start][end]+"}");
                }
                if(debugMode)System.out.print(' ');
                value1 += subestimations[start][end];//iq(myFrequencer.frequency()); was old default implementation without caches.
                if(!fullCalculations && value1>=output) break; // early stop, already more than current min
                start=end;
            }
            if(debugMode)
                if(start==myTarget.length) System.out.println("\tval_"+p+" := "+ value1);
                else System.out.println("\tval_"+p+" >= minCandidate"); // early stop, already more than current min

            // retain the minimal value
            if(value1<output) output=value1;
        }
        if(debugMode)System.out.printf("min_p = %10.5f\n", output);
        return output;
    }

    public static void main(String[] args){
        InformationEstimator myObject;
        InformationEstimator.debugMode = true;
        InformationEstimator.fullCalculations = false;
        myObject=new InformationEstimator();
        myObject.setSpace("3210321001230123".getBytes());

        myObject.setTarget("0".getBytes());
        if(!debugMode) myObject.showVariables();
        System.out.printf("%10.5f", myObject.estimation());
        System.out.printf(" (expected %10.5f)\n\n", 2.0);

        myObject.setTarget("01".getBytes());
        if(!debugMode) myObject.showVariables();
        System.out.printf("%10.5f", myObject.estimation());
        System.out.printf(" (expected %10.5f)\n\n", 3.0);

        myObject.setTarget("0123".getBytes());
        if(!debugMode) myObject.showVariables();
        System.out.printf("%10.5f", myObject.estimation());
        System.out.printf(" (expected %10.5f)\n\n", 3.0);

        myObject.setTarget("00".getBytes());
        if(!debugMode) myObject.showVariables();
        System.out.printf("%10.5f", myObject.estimation());
        System.out.printf(" (expected %10.5f)\n\n", 4.0);
    }
}
