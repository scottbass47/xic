package mtm68.lexer;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class SourceFileLexer {

	private String filename;

	private Lexer lexer;

	private List<Token> tokens;

	public SourceFileLexer(String filename, Path sourcePath) throws FileNotFoundException {
		this.filename = filename;
		this.lexer = createLexer(new FileReader(sourcePath.resolve(filename).toString()));
	}
	
	public SourceFileLexer(String filename) throws FileNotFoundException {
		this.filename = filename;
		this.lexer = createLexer(new FileReader(filename));
	}

	public SourceFileLexer(String filename, Reader reader) {
		this.filename = filename;
		this.lexer = createLexer(reader);
	}
	
	private Lexer createLexer(Reader reader) {
		return new Lexer(reader, new TokenFactory());
	}
	

	/**
	 * Returns list of tokens lexed from the file on which the SourceFileLexer was instantiated on.
	 * 
	 * @return list of tokens
	 * @throws IOException
	 */
	public List<Token> getTokens() throws IOException {
		if (tokens == null) {
			tokens = new ArrayList<>();

			for (Token token = (Token) lexer.next_token(); token.getType() != TokenType.EOF; token = (Token) lexer.next_token()) {
				tokens.add(token);
				if (token.getType() == TokenType.error)
					break;
			}
			lexer.yyclose();
		}
		return tokens;
	}

	public String getFilename() {
		return filename;
	}
}
