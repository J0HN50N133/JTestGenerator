package cut;


public class LoopTest {
    public int isPrime(int i){
        while(i>0){
            if(i>10)
                i -= 2;
            else
                i -= 4;
        }
        return i;
    }

    public boolean isPalindrome(int[] arr) {
        if (arr.length == 0) return false;
        int front = 0;
        int back = arr.length - 1;
        while (front < back) {
            if (arr[front] != arr[back])
                return false;
            front++;
            back--;
        }
        return true;
    }

    public int pat(char[] subject, char[] pattern){
        final int NotFound = -1;
        int iSub = 0;
        int rtnIndex = NotFound;
        boolean isPat = false;
        int subjectLen = subject.length;
        int patternLen = pattern.length;

        while (isPat == false && iSub + patternLen - 1 < subjectLen) {
            if (subject[iSub] == pattern[0]) {
                rtnIndex=iSub;
                isPat = true;
                for (int iPat = 1; iPat < patternLen; iPat++) {
                    if (subject[iSub + iPat] != pattern[iPat]) {
                        rtnIndex = NotFound;
                        isPat = false;
                        break;
                    }
                }
            }
            iSub++;
        }
        return rtnIndex;
    }
    public int raise(int x, int k){
        if (k == 0) return 1;
        if (x == 0) return 0;
        int r = 1;
        if (k > 0) {
            while (k-- != 0) {
                r *= x;
            }
        }
        return r;
    }
    public int foo(int i0, int i1, int i2, int i3, int i4, int i5, boolean b){
        b = !b;
        i2 = i1+i0;
        i1 = i2+i3;
        i4 = i1+i2;
        i5 = i4+i3;
        if (i5 >0)
            return i5;
        return i4;
    }
}
