package s4.B213375; // s4.studentID

import java.lang.*;
import java.util.Arrays;
import s4.specification.*;

/*package s4.specification;
  ここは、１回、２回と変更のない外部仕様である。
  public interface FrequencerInterface {     // This interface provides the design for frequency counter.
  void setTarget(byte  target[]); // set the data to search.
  void setSpace(byte  space[]);  // set the data to be searched target from.
  int frequency(); //It return -1, when TARGET is not set or TARGET's length is zero
  //Otherwise, it return 0, when SPACE is not set or SPACE's length is zero
  //Otherwise, get the frequency of TAGET in SPACE
  int subByteFrequency(int start, int end);
  // get the frequency of subByte of taget, i.e target[start], taget[start+1], ... , target[end-1].
  // For the incorrect value of START or END, the behavior is undefined.
  }
*/

public class Frequencer implements FrequencerInterface{
    byte[] myTarget;
    byte[] mySpace;
    boolean targetReady=false;
    boolean spaceReady=false;

    int[] suffixArray;

    static boolean debug=false;
    static boolean shortcut=true;
    static boolean binSearch=true;

    private void printSuffixArray(){
        if(spaceReady){
            for(int i=0; i<mySpace.length; i++){
                int s=suffixArray[i];
                System.out.printf("suffixArray[%2d]=%2d:", i, s);
                for(int j=s; j<mySpace.length; j++){
                    System.out.write(mySpace[j]);
                }
                System.out.write('\n');
            }
        }
    }
    private int suffixCompare(int i/*<len*/, int j/*<len*/)/*fastOGJ*/{
        /*Compares SPACE.substring(i) with SPACE.substring(j) BYTE-lexicographically
        * (compares as words on a dictionary, where the precedence of each character is
        * defined by its codepoint (ASCII) value; and lower codepoints precede higher ones)
        * returns
        * -1 : in order : substring(i) < ... < substring(j)
        *  0 : exact match
        *  1 : reversed : substring(i) > ... > substring(j)
        * */
        int chcmp = mySpace[i]-mySpace[j];//character_compare (like strcmp)
        if(chcmp!=0) return Integer.signum(chcmp);//trivial case type1/*!*/
        if(i==j) return 0; //trivial case type2/*!*/
        //non-trivial case; long compare:
        int comparableIndices = mySpace.length - Integer.max(i,j);
        for(int n=1; n<comparableIndices/*((n<suff_i.len)&&(n<suff_j.len))*/; n++){
            chcmp = mySpace[i+n]-mySpace[j+n];//character_compare (like strcmp)
            if(chcmp!=0) return Integer.signum(chcmp);
        }//...all comparable indices match
        return (i>j)/*suff_i.len<suff_j.len*/? -1 : 1 ;
    }
    public void setSpace(byte[] space)/*prolly faster ways to sort here*//*OGJ*/{
        this.mySpace = space; if(mySpace.length>0) this.spaceReady = true;
        this.suffixArray = new int[space.length]; //init (unsorted)
        /*  e.g.    for SPACE "ABC"
            suffixArray[0] ((0th in dictionary order)) = start@ 0 (ABC)
            suffixArray[1] ((1st                    )) =      @ 1 (BC)
            suffixArray[2] ((2nd                    )) =      @ 2 (C)
         *  e.g.    for SPACE "ZYYX"
            suffixArray[0] ((0th in dictionary order)) = start@ 3 (X)
            suffixArray[1] ((1st                    )) =      @ 2 (YX)
            suffixArray[2] ((2nd                    )) =      @ 1 (YYX)
            suffixArray[3] ((3rd                    )) =      @ 0 (ZYYX)
        */
        // count1s[substringIndex] := (counted1s)
        int[] count1s = new int[mySpace.length]; //init (all 0s by default)
        for(int i=0; i<mySpace.length; i++){//for all substrings...
            for(int j=0; j<mySpace.length; j++)//...compare against all others:...
                if(suffixCompare(i, j)==1)
                    count1s[i]+=1; //...and count 1s
        }
        // suffixArray[(counted1s)] := substringIndex
        for(int i=0; i<mySpace.length; i++) suffixArray[count1s[i]]=i;
    }

    // ここから始まり、指定する範囲までは変更してはならないコードである。
    public void setTarget(byte[] target){
        myTarget=target; if(myTarget.length>0) targetReady=true;
    }
    public int frequency(){
        if(targetReady==false) return -1;
        if(spaceReady==false) return 0;
        return subByteFrequency(0, myTarget.length);
    }
    public int subByteFrequency(int start, int end){
        // start, and end specify a string to search in myTarget,
        // if myTarget is "ABCD", 
        //     start=0, and end=1 means string "A".
        //     start=1, and end=3 means string "BC".
        // This method returns how many the string appears in my Space.
        // 
        /* This method should be work as follows, but much more efficient.
           int spaceLength = mySpace.length;                      
           int count = 0;                                        
           for(int offset = 0; offset< spaceLength - (end - start); offset++) {
            boolean abort = false; 
            for(int i = 0; i< (end - start); i++) {
             if(myTarget[start+i] != mySpace[offset+i]) { abort = true; break; }
            }
            if(abort == false) { count++; }
           }
        */
        // The following the counting method using suffix array.
        // 演習の内容は、適切なsubByteStartIndexとsubByteEndIndexを定義することである。
        int first=subByteStartIndex(start, end);
        int last1=subByteEndIndex(start, end);
        return last1-first;
    }
    // 変更してはいけないコードはここまで。

    private int targetCompare(int i, int j, int k)/*OGJ*//*Tested*//*1 caveat assumption*/{
        /*  suffix_i = SPACE.substring(i)
                i.e. the substring of SPACE starting at index i
                e.g. SPACE "Hamburger" | i=3 -> "burger"
            target_j_k = TARGET.substring(j,k)
                i.e. the substring of TARGET starting at index j (inclusive) & ending at k (exclusive)
                e.g. TARGET "urge" | j,k=2,4 -> "ge"
        */
        /*Compares SPACE.substring(i) with TARGET.substring(j,k) BYTE-lexicographically
         * (compares as words on a dictionary, where the precedence of each character is
         * defined by its codepoint (ASCII) value; and lower codepoints precede higher ones)
         * BUT if (suffix_i).beginsWith(TARGET_jk) returns 0.
         * returns
         * -1 : in order : suffix_i < ... < TARGET_jk
         *  0 : suffix_i.beginsWith(TARGET_jk)        including case: (suffix_i)==(TARGET_jk)
         *  1 : reversed : suffix_i > ... > TARGET_jk   BUT ONLY IF !(suffix_i).beginsWith(TARGET_jk)
         * */
        // if first part of suffix_i is equal to target_j_k, it returns 0;
        // if suffix_i > target_j_k it return 1;
        // if suffix_i < target_j_k it return -1;
        // Example of search
        // suffix          target
        // "o"       >     "i"
        // "o"       <     "z"
        // "o"       =     "o"
        // "o"       <     "oo"
        // "Ho"      >     "Hi"
        // "Ho"      <     "Hz"
        // "Ho"      =     "Ho"
        // "Ho"      <     "Ho "   : "Ho " is not in the head of suffix "Ho"
        // "Ho"      =     "H"     : "H" is in the head of suffix "Ho"
        // The behavior is different from suffixCompare on this case.
        // For example,
        //    if suffix_i is "Ho Hi Ho", and target_j_k is "Ho", 
        //            targetCompare should return 0;
        //    if suffix_i is "Ho Hi Ho", and suffix_j is "Ho", 
        //            suffixCompare should return -1.
        int suffix_len=mySpace.length-i;
        int target_len=k-j;
        int comparableIndices=Integer.min(suffix_len, target_len);
        for(int n=0; n<comparableIndices; n++){
            int chcmp=mySpace[i+n]-myTarget[j+n];//strcmp(SPACE[i], TARGET[j])={match:0 inorder:NEG reversed:POS}
            if(chcmp<0) return /*Integer.signum(chcmp)*/-1; /*(code correct??)
            if in-order is {SUFFIX, TARGET}
                then "SUFFIX cannot begin with TARGET" && "SUFFIX precedes TARGET"
                so return [negative]*/
            if(chcmp>0) return /*Integer.signum(chcmp*/1; /*(code correct??)
                if in-order is {TARGET, SUFFIX}  &&  not all comparable indices are identical (==we are still inside this for-loop)
                then either
                    (1) "TARGET doesn't match anything in the start of suffix" && "SUFFIX succeeds TARGET" so return [positive]
                    or
                    (2) "the start of TARGET matched the start of SUFFIX, but then it stopped matching" && "SUFFIX succeeds TARGET" so return [positive]
                so return [positive]*/
            if(chcmp==0) continue;
        }
        //reaching here means: SPACE[i~end].startsWith(TARGET[j~k]) ||OR|| TARGET[j~k].startsWith(SPACE[i~end])  ||OR||  (i==s_len)
        if(target_len>suffix_len) return -1;//longTARGET cannot be startingSubstring of shortSUFFIX (not return 0); and shortSTRING precedes longSTRING in order (so return -1)
        else return 0; //shortTARGET is exactly same as head of longSUFFIX; 0 by definition

        //I'm assuming that the case "should return -1" while also "comparable indices = 0" doesn't exist
        //... but I'm not sure if that is mathematically correct
    }
    private int targetCompareRanked(int rank, int j, int k)/*OGJ*//*trivial*/{
        /* suffix_i = SPACE.substring(suffixArray[rank])
            i.e. the substring of SPACE starting at index i=suffixArray[rank]
            NOTE: in a BYTE-Lexicographical ordered list of all suffixes of SPACE, suffix_i is the n-th. */
        return targetCompare(suffixArray[rank],j,k);
    }

    private int subByteStartIndex(int start, int end)/*non-shortcut works*/
    /*can be improved by searching for the index in a non-linear way*/{
        /*  SuffixArrayのなかで、目的の文字列の出現が始まる位置を求めるメソッド。
            ""Returns the index of the first suffix which is
            equal or greater than target_start_end.""
            i.e. the smallest rank r such that strcmp(suffix_i, target.substr(s,e))>=0
                where suffix_i = SPACE.substr(i=suffixArray[r]); the r-th suffix in BYTE-Lexicographic order
        */
        //-----The following are examples assuming SPACE:"Hi Ho Hi Ho"
        /* SuffixArray[r] for "Hi Ho Hi Ho": *note leading whitespaces
            [ 0]= 5: Hi Ho
            [ 1]= 8: Ho
            [ 2]= 2: Ho Hi Ho
            [ 3]= 6:Hi Ho
            [ 4]= 0:Hi Ho Hi Ho
            [ 5]= 9:Ho
            [ 6]= 3:Ho Hi Ho
            [ 7]= 7:i Ho
            [ 8]= 1:i Ho Hi Ho
            [ 9]=10:o
            [10]= 4:o Hi Ho
        */
        /*  EXAMPLE 1:  target : "Ho Ho Ho Ho"
                        start = 0, end = 2
                        target_start_end : "Ho"
            returns 5
            Because SuffixArray[5]:"Ho" is the FIRST entry (searching in sorted order)
            from SuffixArray (the dictionary of suffixes) that MATCHES _OR_ FOLLOWS "Ho".
            i.e. the smallest r such that strcmp(target.substr(s,e),space.substr(suffixArray[r]))>=0
        */
        /*  EXAMPLE 2:  target : "Ho Ho Ho Ho"
                        start = 0, end = 3
                        target_start_end : "Ho_"
            returns 6
            Because SuffixArray[6]:"Ho_" is the FIRST entry (searching in sorted order)
            from SuffixArray (the dictionary of suffixes) that MATCHES _OR_ FOLLOWS "Ho_".
            i.e. the smallest r such that strcmp(target.substr(s,e),space.substr(suffixArray[r]))>=0
            Note that "Ho" DOES NOT MATCH "Ho_" because they are not exactly equal; and
            also that "Ho" DOES NOT FOLLOW "Ho_" because the former is shorter and the
            proper sorted BYTE-Lexicographic order is {...,Hn,...,Ho,Ho_,Ho__,...,Hoa,...}.
        */
        if(binSearch) return binSubByteStartIndex(start,end, 0, suffixArray.length);
        if(shortcut){//no strings instanced ; linear search
            for(int r=0; r<suffixArray.length; r++)
                if(targetCompareRanked(r,start,end)>=0) return r; //how heavy is the overhead of targetCompare?? can this be improved??
            return suffixArray.length;
        }

        String space=new String(this.mySpace);
        String target=new String(this.myTarget);
        String target_start_end=target.substring(start, end);
if(debug)System.out.print("<<<");
        for(int r=0; r<suffixArray.length; r++){
            int cmp=space.substring(suffixArray[r]).compareTo(target_start_end);
if(debug)System.out.print("{"+space.substring(suffixArray[r])+"~"+target.substring(start, end)+"}?"  +  "="+cmp+",");
            if(cmp>=0){
if(debug)System.out.println("...ret r_min="+r+"(i"+suffixArray[r]+")>>>");
                return r;/*smallest i such that target.substr DOES NOT PRECEDE SuffixArray[i]
                    (but note that SA stores indirectly as: start_index of space.substr)*/
            }
        }
if(debug)System.out.println("...compared all; ret r_min= len= "+suffixArray.length+"(no valid i)>>>");
        return suffixArray.length; /*This end-line is reached...
            when target_start_end PRECEDES ALL entries (& particularly the last entry) in
            SuffixArray (the dictionary of suffix substrings of SPACE), also implying that
            there is no match.*/
    }

    private int subByteEndIndex(int start, int end)/*non-shortcut works*/{
        /*  SuffixArrayのなかで、目的の文字列の出現しなくなる場所を求めるメソッド。
            ""Returns the index of the first suffix which is
            greater than target_start_end; (and not equal to target_start_end).""
            i.e. the smallest rank r such that strcmp(suffix_i, target.substr(s,e))>0
                where suffix_i = SPACE.substr(i=suffixArray[r]); the r-th suffix in BYTE-Lexicographic order
         */
        //-----The following are examples assuming SPACE:"Hi Ho Hi Ho"
        /* SuffixArray[r] for "Hi Ho Hi Ho": *note leading whitespaces
            [ 0]= 5: Hi Ho
            [ 1]= 8: Ho
            [ 2]= 2: Ho Hi Ho
            [ 3]= 6:Hi Ho
            [ 4]= 0:Hi Ho Hi Ho
            [ 5]= 9:Ho
            [ 6]= 3:Ho Hi Ho
            [ 7]= 7:i Ho
            [ 8]= 1:i Ho Hi Ho
            [ 9]=10:o
            [10]= 4:o Hi Ho
        */
        /*  EXAMPLE 1:  target : "HoHoHo"
                        start = 0, end = 2
                        target_start_end : "Ho"
            returns 7
            Because SuffixArray[7]:"Ho" is the FIRST entry (searching in sorted order)
            from SuffixArray (the dictionary of suffixes) that
            DOESN'T MATCH _OR_ DOESN'T START WITH "Ho".
            Refer to the following sorted list of strings in ASCII-Lexicographic order:
                SA[3]=6:    Hi_Ho       #ignored...
                SA[4]=0:    Hi_Ho_Hi_Ho #ignored...
                target_s_e: Ho          # <-
                SA[5]=9:    Ho          #STARTSWITH(Ho) (and also EXACTMATCH(Ho))
                SA[6]=3:    Ho_Hi_Ho    #STARTSWITH(Ho)
                SA[7]=7:    i_Ho        #fail! (smallest r such that fail : 7)
                SA[8]=1:    i_Ho_Hi_Ho  #fail!
        */
        /*  EXAMPLE 2:  target : "High and Low"
                        start = 1, end = 2
                        target_start_end : "i"
            returns 9
            Because SuffixArray[9]:"Ho" is the FIRST entry (searching in sorted order)
            from SuffixArray (the dictionary of suffixes) that
            DOESN'T MATCH _OR_ DOESN'T START WITH "Ho".
            Refer to the following sorted list of strings in ASCII-Lexicographic order:
                SA[3]=6:    Hi_Ho       #ignored...
                SA[4]=0:    Hi_Ho_Hi_Ho #ignored...
                SA[5]=9:    Ho          #ignored...
                SA[6]=3:    Ho_Hi_Ho    #ignored...
                target_s_e: i           # <-
                SA[7]=7:    i_Ho        #STARTSWITH(i)
                SA[8]=1:    i_Ho_Hi_Ho  #STARTSWITH(i)
                SA[9]=10:   o           #fail! (smallest r such that fail : 9)
                SA[10]=4:   o_Hi_Ho     #fail!
        */
        /*
         * Note with insight that the relevance of this analysis (as can be seen through
         * the examples) is that the number of cases where
         *   the suffix from SA[x] STARTSWITH(target.substr(start,end))
         *   (including, if it exists, the EXACTMATCH case of SA[x]==target.substr(start,end))
         * is a count of the distinct instances of the substring TARGET.substr(start,end) in
         * the main string SPACE.
         * */
        if(binSearch) return binSubByteEndIndex(start,end, 0, suffixArray.length);
        if(shortcut){//no strings instanced ; linear search
            for(int r=0; r<suffixArray.length; r++)
                if(targetCompareRanked(r,start,end)>0) return r; //how heavy is the overhead of targetCompare?? can this be improved??
                //by spec definition of targetCompare, >0 <=> "SUFFIX doesn't begin with TARGET_s_e"&&"inReversedOrder{SUFFIX,TARGET_s_e}"
            return suffixArray.length;
        }/*!*//*for subByteStartIndex, it's probably a good idea to search non-linearly, with something more clever like a binary search;
        but for subByteEndIndex it's probably (in general) better to search linearly starting with the index returned by subByteStartIndex
        because (in general) there are probably not many SUFFIXes that start with TARGET_s_e
                                          (i.e. not many (r,start,end) such that targetCompare(r,s,e) returns 0)
                                          (i.e. not many copies of TARGET_s_e inside of SPACE)
        so then (in general) subByteEndIndex shouldn't be far after subByteStartIndex*//*but maybe divide and conquer and search
        linearly and binary at the same time (one step each per loop in a shared loop) and see which finishes first??*/

        String space=new String(this.mySpace);
        String target=new String(this.myTarget);
        String target_start_end=target.substring(start, end);
if(debug)System.out.println("///--tse:"+target_start_end);
        int count=0; //suffixes that start with target.substr(s,e)
        int rank_of_first_match_candidate=subByteStartIndex(start,end);
        for(int r=rank_of_first_match_candidate; r<suffixArray.length; r++){
if(debug)System.out.print("--["+r+"]:"+space.substring(suffixArray[r])+".sW(tse)?\t\t"+space.substring(suffixArray[r]).startsWith(target_start_end)+":"+targetCompareRanked(r, start, end));
            if(space.substring(suffixArray[r]).startsWith(target_start_end)){
                count++;
if(debug)System.out.print(" c="+count);
            }
            else{
if(debug)System.out.println("...ret r_max="+r+"(i"+suffixArray[r]+")\\\\\\");
                return r;/*smallest r such that SuffixArray[r] DOES NOT START WITH target_s_e
                    but ignoring indices less than rank_of_first_match_candidate
                    (note that rank_of_first_match_candidate might refer to a string that
                    doesn't start with target_s_e, and it just comes later than target_s_e in
                    ASCII-Lexicographic order)*/
            }
if(debug)System.out.println();
        }
if(debug)System.out.println("ended forloop, all latter ranks start with tse ... ret r_max= len="+suffixArray.length+"(no valid i)\\\\\\");
        return suffixArray.length;/*This end-line is reached...
            when method never entered the 'for' loop since
                subByteStartIndex(start,end) < suffixArray.length   == false
                meaning count==0; or
            when all entries in SuffixArray (the dictionary of suffix substrings of SPACE)
                AFTER AND INCLUDING index = subByteStartIndex(start,end)
                UP UNTIL BUT EXCLUDING index = suffixArray.length
                (i.e. i for: first_match_index <= i < SPACE.length)
                all STARTWITH(target_start_end)
                meaning count==(SPACE.length - subByteStartIndex(...)) */
    }

    private int binSubByteStartIndex(int start, int end, int leftBound, int rightBound){//find first targCompRank(r)>=0 (NON-NEGATIVE)
        //System.out.print("![["+leftBound+"|"+rightBound+"]]nonneg? \t");
        if(leftBound==rightBound || leftBound+1==rightBound){
            boolean leftNonNeg = (targetCompareRanked(leftBound, start,end)>=0);
            int firstNonNeg = (leftNonNeg)? leftBound : leftBound+1;
            //System.out.println("~~~finally: "+firstNonNeg);
            return firstNonNeg;
        }

        int pivot=(leftBound) + (rightBound-leftBound)/2; //r
        int cmpAtPivot=targetCompareRanked(pivot,start,end);
/*
        String spsuf = new String(mySpace).substring(suffixArray[pivot]);
        String tasub = new String(myTarget).substring(start,end);
        System.out.print("@"+leftBound+"+("+rightBound+"-"+leftBound+")/2="+pivot+":eval: "+cmpAtPivot);
        boolean prefixed = spsuf.startsWith(tasub);
        System.out.print(" \t=expects= "+  (prefixed? 0 : spsuf.compareTo(tasub))  );
        System.out.print(" cuz "+(prefixed?
                        ("("+spsuf+").startsWith("+tasub+")")
                        :
                        ("=("+spsuf+").compareTo("+tasub+")")
                ));
        System.out.print("\n\t\t");
 */
        if(!(cmpAtPivot>=0)){//threshold(first NON-NEG) on right
            int newLeft = pivot;
            //System.out.println("goRite-> : [["+newLeft+"|"+rightBound+"]]");
            return binSubByteStartIndex(start, end, newLeft, rightBound);
        }
        else{//threshold(first NON-NEG) on left
            int newRite = pivot;
            //System.out.println("<-goLeft : [["+leftBound+"|"+newRite+"]]");
            return binSubByteStartIndex(start, end, leftBound, newRite);
        }
    }
    private int binSubByteEndIndex(int start, int end, int leftBound, int rightBound){//find first targCompRank(r)>0 (STRICT-POSITIVE)
        //System.out.print("![["+leftBound+"|"+rightBound+"]]pos? \t");
        if(leftBound==rightBound || leftBound+1==rightBound){
            boolean leftPositive = (targetCompareRanked(leftBound, start,end)>0);
            int firstPositive = (leftPositive)? leftBound : leftBound+1;
            //System.out.println("~~~finally: "+firstPositive);
            return firstPositive;
        }

        int pivot=(leftBound) + (rightBound-leftBound)/2; //r
        int cmpAtPivot=targetCompareRanked(pivot,start,end);
/*
        String spsuf = new String(mySpace).substring(suffixArray[pivot]);
        String tasub = new String(myTarget).substring(start,end);
        System.out.print("@"+leftBound+"+("+rightBound+"-"+leftBound+")/2="+pivot+":eval: "+cmpAtPivot);
        boolean prefixed = spsuf.startsWith(tasub);
        System.out.print(" \t=expects= "+  (prefixed? 0 : spsuf.compareTo(tasub))  );
        System.out.print(" cuz "+(prefixed?
                ("("+spsuf+").startsWith("+tasub+")")
                :
                ("=("+spsuf+").compareTo("+tasub+")")
        ));
        System.out.print("\n\t\t");
 */
        if(!(cmpAtPivot>0)){//threshold(first POSITIVE) on right
            int newLeft = pivot;
            //System.out.println("goRite-> : [["+newLeft+"|"+rightBound+"]]");
            return binSubByteEndIndex(start, end, newLeft, rightBound);
        }
        else{//threshold(first POSITIVE) on left
            int newRite = pivot;
            //System.out.println("<-goLeft : [["+leftBound+"|"+newRite+"]]");
            return binSubByteEndIndex(start, end, leftBound, newRite);
        }
    }

    // Suffix Arrayを使ったプログラムのホワイトテストは、
    // privateなメソッドとフィールドをアクセスすることが必要なので、
    // クラスに属するstatic mainに書く方法もある。
    // static mainがあっても、呼びださなければよい。
    // 以下は、自由に変更して実験すること。
    // 注意：標準出力、エラー出力にメッセージを出すことは、
    // static mainからの実行のときだけに許される。
    // 外部からFrequencerを使うときにメッセージを出力してはならない。
    // 教員のテスト実行のときにメッセージがでると、仕様にない動作をするとみなし、
    // 減点の対象である。
    public static void main(String[] args){
        Frequencer f;//frequencerObject;
        try{ // テストに使うのに推奨するmySpaceの文字は、"ABC", "CBA", "HHH", "Hi Ho Hi Ho".
            //Test: "ABC"
            {
                f=new Frequencer();
                f.setSpace("ABC".getBytes());
                System.out.println("SPACE:"+Arrays.toString(f.mySpace));
                f.printSuffixArray();
                System.out.println("Compare [0]vs[1]: "+f.suffixCompare(0, 1)+
                        "\t( expects -1 :: substr(0):ABC precedes(-1) substr(1):BC :: A<B )");
                System.out.println("Compare [1]vs[2]: "+f.suffixCompare(1, 2));
                System.out.println("Compare [2]vs[1]: "+f.suffixCompare(2, 1));
                System.out.println("Compare [2]vs[2]: "+f.suffixCompare(2, 2));
                System.out.println();
            }
            //Test: "CBA"
            {
                f=new Frequencer();
                f.setSpace("CBA".getBytes());
                System.out.println("SPACE:"+Arrays.toString(f.mySpace));
                f.printSuffixArray();
                System.out.println("Compare [0]vs[1]: "+f.suffixCompare(0, 1)+
                        "\t( expects  1 :: substr(0):CBA  follows(1)  substr(1):BA :: C>B )");
                System.out.println("Compare [1]vs[2]: "+f.suffixCompare(1, 2));
                System.out.println("Compare [2]vs[1]: "+f.suffixCompare(2, 1));
                System.out.println("Compare [2]vs[2]: "+f.suffixCompare(2, 2));
                System.out.println();
            }
            //Test: "EDCAB"
            {
                f=new Frequencer();
                f.setSpace("EDCAB".getBytes());
                System.out.println("SPACE:"+Arrays.toString(f.mySpace));
                f.setTarget("CA".getBytes());
                f.printSuffixArray();
                for(int r=0; r<f.mySpace.length; r++){
                    System.out.println(new String(f.mySpace).substring(f.suffixArray[r])+".beginsWith("+new String(f.myTarget)+")?    true=0 else=strcmp()");
                    System.out.println("\ttargetCompare      :"+f.targetCompare(f.suffixArray[r], 0, f.myTarget.length));
                    System.out.println("\ttargetCompareRanked:"+f.targetCompareRanked(r, 0, f.myTarget.length));
                }
                System.out.println();
            }
            //Test: "HHH"
            {
                f=new Frequencer();
                f.setSpace("HHH".getBytes());
                System.out.println("SPACE:"+Arrays.toString(f.mySpace));
                f.printSuffixArray();
                System.out.println();
            }
            //MultiTest: "Hi Ho Hi Ho"
            {
                f=new Frequencer();
                f.setSpace("Hi Ho Hi Ho".getBytes());
                System.out.println("SPACE:"+Arrays.toString(f.mySpace));
                f.printSuffixArray();
            /* Example for "Hi Ho Hi Ho" (expected result): *note leading whitespaces
                [ 0]= 5: Hi Ho      //first in order (index-0) is:  substring[5,end+1) "_Hi_Ho"
                [ 1]= 8: Ho         //second in order (index-1) is: substring[8,end+1) "_Ho"
                [ 2]= 2: Ho Hi Ho
                [ 3]= 6:Hi Ho
                [ 4]= 0:Hi Ho Hi Ho
                [ 5]= 9:Ho
                [ 6]= 3:Ho Hi Ho
                [ 7]= 7:i Ho
                [ 8]= 1:i Ho Hi Ho
                [ 9]=10:o
                [10]= 4:o Hi Ho
            */
                System.out.println();

                //Test substring frequency counter: (space still "Hi Ho Hi Ho")
            /*  SUBTEST 1:  target : "H"
                    SA[1]=8:    _Ho         #ignored...
                    SA[2]=2:    _Ho Hi Ho   #ignored...
                    target_s_e: H           # <-
                    SA[3]=6:    Hi_Ho       #STARTSWITH(H) (smallest r such that not precedes : 3)
                    SA[4]=0:    Hi_Ho_Hi_Ho #STARTSWITH(H)
                    SA[5]=9:    Ho          #STARTSWITH(H)
                    SA[6]=3:    Ho_Hi_Ho    #STARTSWITH(H)
                    SA[7]=7:    i_Ho        #fail! (smallest r such that fail : 7)
                    SA[8]=1:    i_Ho_Hi_Ho  #fail!
            */
                f.setTarget("H".getBytes());
                System.out.println("TARGET:"+new String(f.myTarget));
boolean debug_before_override=debug;
debug=true;System.out.println("DEBUG_OVERRIDE_ON");
                int result=f.frequency();//How many distinct instances of tgt:"H" in spc:"Hi Ho Hi Ho"?
debug=debug_before_override;System.out.println("DEBUG_OVERRIDE_CEDED");
                System.out.print("Freq = "+result+" ");
                System.out.println((result==4)? "OK" : "WRONG");
                System.out.println();

            /*  SUBTEST 2:  target : "HoHoHo"
                            start = 0, end = 2
                            target_start_end : "Ho"
                    SA[3]=6:    Hi_Ho       #ignored...
                    SA[4]=0:    Hi_Ho_Hi_Ho #ignored...
                    target_s_e: Ho          # <-
                    SA[5]=9:    Ho          #STARTSWITH(Ho) (smallest i such that not precedes : 5)
                    SA[6]=3:    Ho_Hi_Ho    #STARTSWITH(Ho)
                    SA[7]=7:    i_Ho        #fail! (smallest i such that fail : 7)
                    SA[8]=1:    i_Ho_Hi_Ho  #fail!
            */
                f.setTarget("HoHoHo".getBytes());
                System.out.println("TARGET:"+new String(f.myTarget));
                System.out.println("TARGET.substr:"+new String(f.myTarget).substring(0, 2));
                result=f.subByteFrequency(0, 2);
                System.out.print("Freq.sub = "+result+" ");
                System.out.println((result==2)? "OK" : "WRONG");
                System.out.println();

            /*  SUBTEST 2:  target : "High and Low"
                            start = 1, end = 2
                            target_start_end : "i"
                    SA[5]=9:    Ho          #ignored...
                    SA[6]=3:    Ho_Hi_Ho    #ignored...
                    target_s_e: i           # <-
                    SA[7]=7:    i_Ho        #STARTSWITH(i) (smallest i such that not precedes : 7)
                    SA[8]=1:    i_Ho_Hi_Ho  #STARTSWITH(i)
                    SA[9]=10:   o           #fail! (smallest i such that fail : 9)
                    SA[10]=4:   o_Hi_Ho     #fail!
            */
                f.setTarget("High and Low".getBytes());
                System.out.println("TARGET:"+new String(f.myTarget));
                System.out.println("TARGET.substr:"+new String(f.myTarget).substring(1, 2));
                result=f.subByteFrequency(1, 2);
                System.out.print("Freq.sub = "+result+" ");
                System.out.println((result==2)? "OK" : "WRONG");
                System.out.println();
            }

            //Future tests for efficiency reforms:
            //DeepTest: "HoHoHo" v "Ho"
            {
                f=new Frequencer();
                f.setSpace("HoHoHo".getBytes());
                System.out.println("SPACE:"+Arrays.toString(f.mySpace));
                System.out.println("SPACE:"+new String(f.mySpace));
                f.setTarget("Ho".getBytes());
                int j=0, k=2;
                System.out.println("TARGET.substr:"+new String(f.myTarget).substring(j, k));
                int r;
                for(r=0; r<f.mySpace.length; r++){
                    int suffix_len=f.mySpace.length-f.suffixArray[r];
                    int target_len=k-j;
                    int comparableIndices=Integer.min(suffix_len, target_len);
                    System.out.println("check in suffix::"+new String(f.mySpace).substring(f.suffixArray[r]));
                    System.out.println("r"+r+"\ts_len"+suffix_len+"\tt_len"+target_len+"\tcompble"+comparableIndices);
                    int n;
                    boolean aborted=false;
                    for(n=0; n<comparableIndices; n++){
                        int strcmp=f.mySpace[f.suffixArray[r]+n]-f.myTarget[j+n];//strcmp(SPACE[i], TARGET[j])={match:0 inorder:NEG reversed:POS}
                        System.out.println("\tn="+n+": strcmp="+strcmp);
                        if(strcmp!=0){
                            System.out.println(Integer.signum(strcmp));
                            aborted=true;
                            break;
                        }
                    }
                    //reaching here means: (!aborted):==:deepmatch
                    if(!aborted){
                        System.out.print("deepmatch&...");
                        if(target_len<=suffix_len)//shortTARGET deepmatched longerSUFFIX
                            System.out.println("0 cuz SUFFIX.startsWith(TARGET)"+
                                    (new String(f.mySpace).substring(f.suffixArray[r]).startsWith(new String(f.myTarget).substring(j,k)))+
                                    "::expectsTRUE");// true=SPACE[i(=sA[r])~end].startsWith(TARGET[j~k])
                        else//SUFFIX_i is shorter than TARGET_jk, so they are in order(-1);
                            //comparableIndices == suffix_len < target_len ; but also SPACE[i(=sA[r])~end]==TARGET[j:(j+suffix_len)<k]
                            //therefore true=TARGET[j~k].startsWith(SPACE[i~end])
                            System.out.println("-1 cuz long TARGET cannot be starting_substring of SUFFIX: "+
                                    new String(f.mySpace).substring(f.suffixArray[r])+"(!contains)"+
                                    new String(f.myTarget).substring(j, k)+
                                    "  "+
                                    (new String(f.mySpace).substring(f.suffixArray[r]).contains(new String(f.myTarget).substring(j,k)))+
                                    "::expectsFALSE");
                    }
                    System.out.println("____ & implementation said: "+f.targetCompareRanked(r, j, k));
                }
                System.out.println("So, in summary, with:");
                f.printSuffixArray();
                System.out.println(new String(f.mySpace)+":"+new String(f.myTarget)+"["+j+","+k+")");
                System.out.println("matches in ranks ["+f.subByteStartIndex(j, k)+"~"+f.subByteEndIndex(j, k)+")");
                System.out.println();
            }
            //DeepTest: "XYZXXYAYZX" v "XY"
            {
                f=new Frequencer();
                f.setSpace("XYZXXYAYZX".getBytes());
                System.out.println("SPACE:"+Arrays.toString(f.mySpace));
                System.out.println("SPACE:"+new String(f.mySpace));
                f.setTarget("aXYasd".getBytes());
                int j=1, k=3;
                System.out.println("TARGET.substr:"+new String(f.myTarget).substring(j, k));
                int r;
                for(r=0; r<f.mySpace.length; r++){
                    int suffix_len=f.mySpace.length-f.suffixArray[r];
                    int target_len=k-j;
                    int comparableIndices=Integer.min(suffix_len, target_len);
                    System.out.println("check in suffix::"+new String(f.mySpace).substring(f.suffixArray[r]));
                    System.out.println("r"+r+"\ts_len"+suffix_len+"\tt_len"+target_len+"\tcompble"+comparableIndices);
                    int n;
                    boolean aborted=false;
                    for(n=0; n<comparableIndices; n++){
                        int strcmp=f.mySpace[f.suffixArray[r]+n]-f.myTarget[j+n];//strcmp(SPACE[i], TARGET[j])={match:0 inorder:NEG reversed:POS}
                        System.out.println("\tn="+n+": strcmp="+strcmp);
                        if(strcmp!=0){
                            System.out.println(Integer.signum(strcmp));
                            aborted=true;
                            break;
                        }
                    }
                    //reaching here means: (!aborted):==:deepmatch
                    if(!aborted){
                        System.out.print("deepmatch&...");
                        if(target_len<=suffix_len)//shortTARGET deepmatched longerSUFFIX
                            System.out.println("0 cuz SUFFIX.startsWith(TARGET)"+
                                    (new String(f.mySpace).substring(f.suffixArray[r]).startsWith(new String(f.myTarget).substring(j,k)))+
                                    "::expectsTRUE");// true=SPACE[i(=sA[r])~end].startsWith(TARGET[j~k])
                        else//SUFFIX_i is shorter than TARGET_jk, so they are in order(-1);
                            //comparableIndices == suffix_len < target_len ; but also SPACE[i(=sA[r])~end]==TARGET[j:(j+suffix_len)<k]
                            //therefore true=TARGET[j~k].startsWith(SPACE[i~end])
                            System.out.println("-1 cuz long TARGET cannot be starting_substring of SUFFIX: "+
                                    new String(f.mySpace).substring(f.suffixArray[r])+"(!contains)"+
                                    new String(f.myTarget).substring(j, k)+
                                    "  "+
                                    (new String(f.mySpace).substring(f.suffixArray[r]).contains(new String(f.myTarget).substring(j,k)))+
                                    "::expectsFALSE");
                    }
                    System.out.println("____ & implementation said: "+f.targetCompareRanked(r, j, k));
                }
                System.out.println("So, in summary, with:");
                f.printSuffixArray();
                System.out.println(new String(f.mySpace)+":"+new String(f.myTarget)+"["+j+","+k+")");
                System.out.println("matches in ranks ["+f.subByteStartIndex(j, k)+"~"+f.subByteEndIndex(j, k)+")");
                System.out.println();
            }

        }catch(Exception e){
            System.out.println("STOP");
        }
        System.out.println("END");
    }
}
