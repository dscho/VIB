#!/usr/bin/ruby -w

require 'getoptlong'

def usage
	print <<EOF
Usage: compare-stacks [OPTION] <fileA> <fileB>"

 -t <SUBSTRING>, --title-matches=<SUBSTRING>
                    Only use images whose titles match <SUBSTRING>
 -c --close-others
                    Close all other images that might be open, so
                    we're just left with the overlay
EOF
end
  
options = GetoptLong.new(
  [ "--title-matches", "-t", GetoptLong::REQUIRED_ARGUMENT ],
  [ "--close-others",  "-c", GetoptLong::NO_ARGUMENT ]
)

substring = ""
close_others = false

begin
	options.each do |opt,arg|
          case opt
          when "--title-matches"
                  substring = arg
          when "--close-others"
                  close_others = true
          end
        end
rescue
	puts "Bad command line opion: #{$!}\n"
	usage
	exit
end
		
# It's a bit painful getting the escaping right for doing
# this from the shell, so this is a small helper program.

vib_directory=File.dirname(File.expand_path(__FILE__))

memory="512m"

unless ARGV.length == 2
	usage
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

close_string = ""
if close_others
  close_string = " close=1"
end

Dir.chdir( vib_directory ) {

        command = [ "java",
                    "-Xmx#{memory}",
                    "-Dplugins.dir=.",
                    "-jar", "../ImageJ/ij.jar",
                    "-port0",
                    fileA, fileB,
                    "-eval", "run('Overlay Registered','substring=#{substring}#{close_string}');" ]

        puts "Running: "+command.join(' ')
  
	result = system(*command)
	unless result
		puts "Running ImageJ failed."
	end
}

