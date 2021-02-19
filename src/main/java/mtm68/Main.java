package mtm68;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ParserProperties;

import mtm68.lexer.Lexer.Token;
import mtm68.lexer.Lexer.TokenType;
import mtm68.lexer.SourceFileLexer;

public class Main {

	@Option(name="--help", help=true, usage="print help screen")
	private boolean help = false;
	
	@Option(name="--lex", usage="outputs lexed version of source file")
	private boolean lex;
	
	@Option(name="-D", usage="specify location for generated diagnostic files")
	private Path dPath = Paths.get(System.getProperty("user.dir"));
	
	@Argument
	private List<String> sourceFiles = new ArrayList<>();
	
	public static void main(String[] args) {
		new Main().parseCmdLine(args);
	}
	
	public void parseCmdLine(String[] args) {
		CmdLineParser cmdParser = new CmdLineParser(this, ParserProperties.defaults().withShowDefaults(false));
		
		try {
			cmdParser.parseArgument(args);
		}
		catch(CmdLineException e) {
			 System.out.println(e.getMessage());
	         printHelpScreen(cmdParser);
		}
		
		//Act on command arguments		
		if(help || sourceFiles.isEmpty()) printHelpScreen(cmdParser);
		
		// TODO: Ignore non *.xi files
		// TODO: Figure out if it should throw an error or not
		// TODO: Output tokens into lexed file
		if(lex) {
			for(String filename : sourceFiles) {
				System.out.println("Lexing " + filename + " into " + dPath.getFileName());
				try {
					SourceFileLexer lexer = new SourceFileLexer(filename);
					List<Token> tokens = lexer.getTokens();
					tokens.forEach(System.out::println);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public void printHelpScreen(CmdLineParser parser) {
		System.out.println("xic [options...] arguments...");
        parser.printUsage(System.out);
        System.out.println();
	}
}
