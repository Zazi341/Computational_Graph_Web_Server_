package configs;

/**
 * Utility class for bitwise operations on decimal numbers.
 * Converts decimal values to integers, performs bitwise operations, and converts back to decimal.
 */
public class BitwiseUtils {
    
    /**
     * Performs bitwise AND operation on two decimal numbers.
     * @param a First decimal number
     * @param b Second decimal number
     * @return Result of bitwise AND operation as decimal
     */
    public static double bitwiseAnd(double a, double b) {
        if (!isValidInput(a) || !isValidInput(b)) {
            return Double.NaN;
        }
        
        int intA = decimalToInteger(a);
        int intB = decimalToInteger(b);
        int result = intA & intB;
        
        return integerToDecimal(result);
    }
    
    /**
     * Performs bitwise OR operation on two decimal numbers.
     * @param a First decimal number
     * @param b Second decimal number
     * @return Result of bitwise OR operation as decimal
     */
    public static double bitwiseOr(double a, double b) {
        if (!isValidInput(a) || !isValidInput(b)) {
            return Double.NaN;
        }
        
        int intA = decimalToInteger(a);
        int intB = decimalToInteger(b);
        int result = intA | intB;
        
        return integerToDecimal(result);
    }
    
    /**
     * Performs bitwise XOR operation on two decimal numbers.
     * @param a First decimal number
     * @param b Second decimal number
     * @return Result of bitwise XOR operation as decimal
     */
    public static double bitwiseXor(double a, double b) {
        if (!isValidInput(a) || !isValidInput(b)) {
            return Double.NaN;
        }
        
        int intA = decimalToInteger(a);
        int intB = decimalToInteger(b);
        int result = intA ^ intB;
        
        return integerToDecimal(result);
    }
    
    /**
     * Performs bitwise NOT operation on a decimal number.
     * @param a Decimal number to negate
     * @return Result of bitwise NOT operation as decimal
     */
    public static double bitwiseNot(double a) {
        if (!isValidInput(a)) {
            return Double.NaN;
        }
        
        int intA = decimalToInteger(a);
        int result = ~intA;
        
        return integerToDecimal(result);
    }
    
    /**
     * Converts decimal number to integer with proper handling of negative numbers.
     * @param decimal The decimal number to convert
     * @return Integer representation
     */
    private static int decimalToInteger(double decimal) {
        // Handle edge cases
        if (Double.isNaN(decimal) || Double.isInfinite(decimal)) {
            return 0;
        }
        
        // Convert to int, handling overflow by clamping to int range
        if (decimal > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        } else if (decimal < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        
        return (int) decimal;
    }
    
    /**
     * Converts integer back to decimal format.
     * @param integer The integer to convert
     * @return Double representation
     */
    private static double integerToDecimal(int integer) {
        return (double) integer;
    }
    
    /**
     * Validates input for numeric ranges and edge cases.
     * @param value The value to validate
     * @return true if valid, false otherwise
     */
    private static boolean isValidInput(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }
}