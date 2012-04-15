import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@MongoCollection("empl")
class Employee {
	@MongoField String name;
	@MongoField("salary") double yearlySalary; // must be double not float
	@MongoField Employee manager; // must avoid cyclic pickling
	@MongoField List<Project> projects; // must avoid cyclic pickling
	@MongoField Set<String> books;
	int ignoredField;

	public Employee() {
		name = "Anonymous";
		yearlySalary = 100.00;
		manager = null;
		projects = new ArrayList<Project>();
		books = new HashSet<String>();
	}

	public String toString() {
		String retval = "\n\t{\n\t\t'name':'" + name;
		retval  += "',\n\t\t'yearlySalary':'" + yearlySalary;
		if (manager != null)
			retval		+= "',\n\t\t'manager':'" + manager.name;
		else
			retval += "',\n\t\t'manager':'null";
		retval += "',\n\t\t'books':" + Arrays.toString(books.toArray());
		retval += "',\n\t\t'projects'" + projects + "'\n\t}\n";
		return retval;
	}
}