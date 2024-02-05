// While Loops

public class While {
	public static void main(String[] args) {
		int a = 10;
		while (a > 0) {
			a--;
			System.out.println("a is " + a);
		}
	}
}

// Do While Loops

public class DoWhile {
	public static void main(String[] args) {
		// runs at least once
		int a = 0;
		do {
			System.out.println("a is greater than zero");
		} while (a > 0);

		a = 10;
		do {
			System.out.println("a is " + a);
			a--;
		} while (a > 0);
	}
}

// For Loop

public class For {
	public static void main(String[] args) {
		// you don't normally declare the increment like this
		// but I'm doing it for sake of example
		int increment = 1;// <--change this value and see how it changes
		for (int i = 0; i < 10; i += increment) {
			System.out.println("i is " + i);
		}
	}
}

// Foreach Loop

import java.util.Arrays;
import java.util.List;

public class Main {
	public static void main(String[] args) {
		// in java we define arrays with []
		// note the parameter in the main method
		String[] arr = new String[] { "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday" };
		// no I don't know why I chose to do days of the week

		// this will look a bit backwards if you come from other language backgrounds
		// note we take the array first, then we get the value "as" the next variable we
		// declare
		for (String day : arr) {
			System.out.println(day);
		}

		// newer in Java some iterator types have a built in forEach
		// regular arrays don't but the class collections do
		// you'll need to import other parts of the library (see the top of the file)
		List<String> days = Arrays.asList(arr);
		days.forEach((day) -> {
			System.out.println(day);
		});
	}
	int count = arr.length;//FWIW this is similar syntax to javascript arrays
		System.out.println("The array has " + count +" elements");
		for(int i = 0; i < count; i++){
			System.out.println(arr[i]); 
		}
}

// Loop Break

public class Break {
    public static void main(String[] args) {
        while (true) {
            if (true) {
                break;
            }
            System.out.println("I'm loopin'");
        }
        System.out.println("We broke out of the loop");
        /*
         * The code above is really pointless, it's the equivalent if we just ran line
         * 7.
         * It'll try to loop due to true
         * but will exit at line 4 so won't even complete the loop.
         * Your IDE may even highlight line 10 with "dead code" message.
         */
    }
}

// Continue Loop

public class Continue {
	public static void main(String[] args) {
		int number = 0;
		while (number < 20) {
			number++;
			if (number == 5) {
				continue;
			}
			//number++;
			// see what happens if we move number++; here (don't forget to comment out line
			// 5 before trying)
			System.out.println("Number: " + number);
		}
		System.out.println("Done looping");//for the comment on line 10, notice the output and notice that the program doesn't terminate
	}
}