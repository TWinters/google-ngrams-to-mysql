package be.thomaswinters.googlengrams;

import java.net.URISyntaxException;
import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Class that can connect to a n-gram database and constructs simple add and get
 * queries.
 * 
 * @author Thomas Winters
 *
 */
public class NgramMySQLConnector {

	public static final long AMOUNT_OF_1GRAMS_2008 = 561087129l;

	/*-********************************************-*
	 *  Instance variables
	*-********************************************-*/
	private final Connection connection;

	private final int n;
	private final String addCountQuery;
	private final String getCountQuery;

	/*-********************************************-*/

	/*-********************************************-*
	 *  Constructor
	*-********************************************-*/
	public NgramMySQLConnector(int n, Connection connection)
			throws ClassNotFoundException, URISyntaxException, SQLException {
		this.connection = connection;
		this.n = n;
		this.addCountQuery = buildAddQuery(n);
		this.getCountQuery = buildGetQuery(n);
	}

	public NgramMySQLConnector(int n, String host, int port, String username, String password, String databaseName)
			throws ClassNotFoundException, URISyntaxException, SQLException {
		this(n, createConnection(host, port, username, password, databaseName));
	}
	/*-********************************************-*/

	/*-********************************************-*
	 *  Creating the connection
	*-********************************************-*/
	public static Connection createConnection(String host, int port, String username, String password,
			String databaseName) throws ClassNotFoundException, URISyntaxException, SQLException {
		// Create driver
		Class.forName("com.mysql.jdbc.Driver");

		String jdbUrl = "jdbc:mysql://" + host + ":" + port + "/" + databaseName;// +
																					// jdbUri.getPath();
		return DriverManager.getConnection(jdbUrl, username, password);
	}

	protected Connection getConnection() {
		return connection;
	}
	/*-********************************************-*/

	/*-********************************************-*
	 *  Query building
	*-********************************************-*/

	private String buildAddQuery(int n) {
		if (n == 1) {
			return "insert into 1grams (word1, count) values (?, ?)";
		}
		StringBuilder b = new StringBuilder();
		b.append("insert into ");
		b.append(getDatabaseName());
		b.append(" (");
		for (int i = 1; i <= n; i++) {
			b.append("word" + i + ", ");
		}
		b.append("count) values (");
		for (int i = 1; i <= n + 1; i++) {
			b.append("?");
			if (i <= n) {
				b.append(", ");
			}
		}
		b.append(")");
		return b.toString();
	}

	private String buildGetQuery(int n) {
		// SELECT * FROM 2grams WHERE word1 LIKE ? AND word2 LIKE ?
		StringBuilder b = new StringBuilder();
		b.append("SELECT * FROM ");
		b.append(getDatabaseName());
		b.append(" WHERE");
		for (int i = 1; i <= n; i++) {
			if (i > 1) {
				b.append(" AND");
			}
			b.append(" word" + i + " LIKE ?");
		}
		return b.toString();
	}
	/*-********************************************-*/

	/*-********************************************-*
	 *  Mutators
	*-********************************************-*/

	public void addCount(List<String> words, long count) {
		try {
			// create the mysql insert preparedstatement
			PreparedStatement addPS = getConnection().prepareStatement(addCountQuery);
			for (int i = 1; i <= n; i++) {
				addPS.setString(i, words.get(i - 1));
			}
			addPS.setLong(n + 1, count);

			// execute the preparedstatement
			addPS.execute();
		} catch (Exception e) {
			System.err.println("Got an exception!");
			e.printStackTrace();
		}
	}

	public void addAllCount(Map<List<String>, Long> map) {
		// INSERT INTO tableName
		// (column1,column2,column3,column4)
		// VALUES
		// ('value1' , 'value2', 'value3','value4'),
		// ('value1' , 'value2', 'value3','value4'),
		// ('value1' , 'value2', 'value3','value4');

		StringBuilder b = new StringBuilder();
		b.append("INSERT INTO " + getDatabaseName() + " (");
		for (int i = 1; i <= n; i++) {
			b.append("word");
			b.append(i);
			b.append(", ");
		}
		b.append("count) VALUES ");
		String values = map.entrySet().stream()
				.map(e -> "(" + e.getKey().stream().map(f -> "'" + f + "'").collect(Collectors.joining(",")) + ", "
						+ e.getValue() + ")")
				.collect(Collectors.joining(", ")) + ";";
		b.append(values);

		try {
			getConnection().createStatement().execute(b.toString());
		} catch (SQLException e1) {
			System.err.println("Got an exception with query!" + b);
			e1.printStackTrace();
			throw new RuntimeException(e1);
		}

	}
	/*-********************************************-*/

	/*-********************************************-*
	 *  Getters
	*-********************************************-*/
	public ResultSet getRows(List<String> words) throws SQLException {
		// create the mysql insert preparedstatement
		PreparedStatement getCountPS = getConnection().prepareStatement(getCountQuery);
		for (int i = 1; i <= n; i++) {
			getCountPS.setString(i, words.get(i - 1));
		}

		// execute the preparedstatement
		return getCountPS.executeQuery();
	}

	public ResultSet getCustomQuery(String query, String... args) throws SQLException {
		// create the mysql insert preparedstatement
		PreparedStatement preparedStatement = getConnection().prepareStatement(query);
		for (int i = 1; i <= args.length; i++) {
			preparedStatement.setString(i, args[i - 1]);
		}

		// execute the preparedstatement
		return preparedStatement.executeQuery();
	}
	/*-********************************************-*/

	/*-********************************************-*
	 *  Helpers
	*-********************************************-*/
	public String getDatabaseName() {
		return n + "grams";
	}

	/*-********************************************-*/

	public static void main(String[] args) throws ClassNotFoundException, URISyntaxException, SQLException {
		new NgramMySQLConnector(1, System.getenv("ngram_db_host"), Integer.parseInt(System.getenv("ngram_db_port")),
				System.getenv("ngram_db_username"), System.getenv("ngram_db_password"),
				System.getenv("ngram_db_databaseName"));
	}

	public void close() throws SQLException {
		connection.close();
	}

	public int getN() {
		return n;
	}

}
