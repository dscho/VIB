#!/usr/bin/ruby -w

# It's a bit painful getting the escaping right for doing
# this from the shell, so this is a small helper program.

vib_directory=File.dirname(File.expand_path(__FILE__))

require 'getoptlong'
options = GetoptLong.new(
  [ "--help", "-h", GetoptLong::NO_ARGUMENT ],
  [ "--substring", "-s", GetoptLong::REQUIRED_ARGUMENT ]
)

program_name = "compare-stacks"
memory="512m"

def usage
  print <<EOUSAGE
Usage: #{program_name} [OPTION]...

  -h, --help           Display this message and exit.
  -s <SUBSTRING>, --substring=<SUBSTRING>
                       Only use the images with <SUBSTRING> in their
                       titles.
  -k --keep-sources    Keep source images
  -c --close-others    Close non-matching images
EOUSAGE
end

substring = ""

keep_sources = false
close_others = false

begin
  options.each do |opt, arg|
    case opt
    when "--help"
      usage
      exit
    when "--substring"
      substring = arg
    when "--keep-sources"
      keep_sources = true
    when "--close-others"
      close_others = true
    end
  end
rescue
  print "Bad command line option: " + $! + "\n"
  usage
  exit
end

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

macro_options = "substring=#{substring}"
macro_options += ",keep" if keep_sources
macro_options += ",close" if close_others

Dir.chdir( vib_directory ) {

	result = system( "java", "-Xmx#{memory}", "-Dplugins.dir=.", "-jar", "../ImageJ/ij.jar", "-port0", fileA, fileB, "-eval", "run('Overlay Registered','#{macro_options}');" )
	unless result
		puts "Running ImageJ failed."
	end
}
