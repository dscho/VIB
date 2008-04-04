#!/usr/bin/ruby -w

# It's a bit painful getting the escaping right for doing
# this from the shell, so this is a small helper program.

vib_directory=File.dirname(File.expand_path(__FILE__))

memory="512m"

unless ARGV.length == 2
	puts "Usage: compare-stacks <fileA> <fileB>"
	exit( -1 )
end

fileA=ARGV[0]
fileB=ARGV[1]

unless FileTest.exist? fileA
	puts "File '#{fileA}' does not exist."
	exit( -1 )
end

unless FileTest.exist? fileB
	puts "File '#{fileB}' does not exist."
	exit( -1 )
end
	
fileA=File.expand_path(fileA)
fileB=File.expand_path(fileB)

Dir.chdir( vib_directory ) {

	result = system( "java", "-Xmx#{memory}", "-Dplugins.dir=.", "-jar", "../ImageJ/ij.jar", "-port0", fileA, fileB, "-eval", "run('Overlay Registered','');" )
	unless result
		puts "Running ImageJ failed."
	end
}

