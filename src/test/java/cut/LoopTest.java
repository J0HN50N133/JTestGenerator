package cut;


public class LoopTest {
    public boolean isPrime(int i){
        if (i <= 1){
            return false;
        }
        for (int j = 2; j < i; j++){
            if (i % j == 0)
                return false;
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
        int r = 1;
        if (k > 0) {
            while (k-- != 0) {
                r *= x;
            }
        }
        return r;
    }
}
