public class Employee {
	private String type;
	private String name;
	private String id;
	private String age;

	// Getters and setters
	public String getType() { return type; }
	public void setType(String type) { this.type = type; }

	public String getName() { return name; }
	public void setName(String name) { this.name = name; }

	public String getId() { return id; }
	public void setId(String id) { this.id = id; }

	public String getAge() { return age; }
	public void setAge(String age) { this.age = age; }

	@Override
	public String toString() {
		return "Employee [type=" + type + ", name=" + name + ", id=" + id + ", age=" + age + "]";
	}
}
