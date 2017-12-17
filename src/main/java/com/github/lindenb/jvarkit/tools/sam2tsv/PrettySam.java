/*
The MIT License (MIT)

Copyright (c) 2017 Pierre Lindenbaum

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

*/
package com.github.lindenb.jvarkit.tools.sam2tsv;

import java.io.File;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

import com.beust.jcommander.Parameter;
import com.github.lindenb.jvarkit.tools.misc.IlluminaReadName;
import com.github.lindenb.jvarkit.util.bio.fasta.ContigNameConverter;
import com.github.lindenb.jvarkit.util.bio.fasta.ReferenceGenome;
import com.github.lindenb.jvarkit.util.bio.fasta.ReferenceGenomeFactory;
import com.github.lindenb.jvarkit.util.bio.fasta.ContigNameConverter.OnNotFound;
import com.github.lindenb.jvarkit.util.bio.fasta.ReferenceContig;
import com.github.lindenb.jvarkit.util.jcommander.Launcher;
import com.github.lindenb.jvarkit.util.jcommander.Program;
import com.github.lindenb.jvarkit.util.log.Logger;
import com.github.lindenb.jvarkit.util.swing.ColorUtils;

import htsjdk.samtools.Cigar;
import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFlag;
import htsjdk.samtools.SAMProgramRecord;
import htsjdk.samtools.SAMReadGroupRecord;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.util.CloseableIterator;
import htsjdk.samtools.util.CloserUtil;
import htsjdk.samtools.util.ProgressLoggerInterface;
import htsjdk.samtools.util.StringUtil;
/**
BEGIN_DOC

## About long reads

The htsjdk currently (2012-12-16) doesn't support more th 65635 cigar operations.

## Example

```

$ java -jar dist/prettysam.jar -R ref.fa S1.bam

>>>>> 1
          Read-Name : rotavirus_1_317_5:0:0_7:0:0_2de
               Flag : 99
             read paired : 1
             proper pair : 2
     mate reverse strand : 32
           first of pair : 64
               MAPQ : 60
             Contig : rotavirus  (index:0)
              Start : 1
                End : 70
             Strand : +
        Insert-Size : 317
        Mate-Contig : rotavirus  (index:0)
         Mate-Start : 248
        Mate-Strand : -
         Read Group : 
                      ID : S1
                      SM : S1
        Read-Length : 70
              Cigar : 70M (N=1)
           Sequence : 
                Read (0) : GGCTTTTAAT GCTTTTCAGT GGTTGCTGCT CAATATGGCG TCAACTCAGC AGATGGTCAG
                     Mid : |||||||||| |||||||||| |||||||||| ||| |||| | || ||||||| ||||||| ||
                 Ref (1) : GGCTTTTAAT GCTTTTCAGT GGTTGCTGCT CAAGATGGAG TCTACTCAGC AGATGGTAAG
                      Op : MMMMMMMMMM MMMMMMMMMM MMMMMMMMMM MMMMMMMMMM MMMMMMMMMM MMMMMMMMMM
                    Qual : ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++
                 Ref-Pos : 1          11         21         31         41         51        

               Read (60) : CTCTAATATT
                     Mid : ||||| ||||
                Ref (61) : CTCTATTATT
                      Op : MMMMMMMMMM
                    Qual : ++++++++++
                 Ref-Pos : 61        

               Tags : 
                      MD :  33G4A3T14A7T4   "String for mismatching positions"
                      NM :  5   "Edit distance to the reference"
                      AS :  45   "Alignment score generated by aligner"
                      XS :  0   "Reserved for end users"
<<<<< 1

>>>>> 2
          Read-Name : rotavirus_1_535_4:0:0_4:0:0_1a6
               Flag : 163
             read paired : 1
             proper pair : 2
     mate reverse strand : 32
          second of pair : 128
               MAPQ : 60
             Contig : rotavirus  (index:0)
              Start : 1
                End : 70
             Strand : +
        Insert-Size : 535
        Mate-Contig : rotavirus  (index:0)
         Mate-Start : 466
        Mate-Strand : -
         Read Group : 
                      ID : S1
                      SM : S1
        Read-Length : 70
              Cigar : 70M (N=1)
           Sequence : 
                Read (0) : GGCTTTTACT GCTTTTCAGT GGTTGCTTCT CAAGATGGAG TGTACTCATC AGATGGTAAG
                     Mid : |||||||| | |||||||||| ||||||| || |||||||||| | |||||| | ||||||||||
                 Ref (1) : GGCTTTTAAT GCTTTTCAGT GGTTGCTGCT CAAGATGGAG TCTACTCAGC AGATGGTAAG
                      Op : MMMMMMMMMM MMMMMMMMMM MMMMMMMMMM MMMMMMMMMM MMMMMMMMMM MMMMMMMMMM
                    Qual : ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++
                 Ref-Pos : 1          11         21         31         41         51        

               Read (60) : CTCTATTATT
                     Mid : ||||||||||
                Ref (61) : CTCTATTATT
                      Op : MMMMMMMMMM
                    Qual : ++++++++++
                 Ref-Pos : 61        

               Tags : 
                      MD :  8A18G13C6G21   "String for mismatching positions"
                      NM :  4   "Edit distance to the reference"
                      AS :  50   "Alignment score generated by aligner"
                      XS :  0   "Reserved for end users"
<<<<< 2
```

END_DOC
 */
@Program(name="prettysam",
description="Pretty SAM alignments",
keywords={"sam","bam",}
)
public class PrettySam extends Launcher {
	private static final Logger LOG = Logger.build(PrettySam.class).make();
	@Parameter(names={"-o","--output"},description=OPT_OUPUT_FILE_OR_STDOUT)
	private File outputFile = null;
	@Parameter(names={"-r","-R","--reference"},description=ReferenceGenomeFactory.OPT_DESCRIPTION)
	private String referenceUri = null ;
	@Parameter(names={"--no-unicode"},description="disable unicode to display ascii histogram")
	private boolean disable_unicode=false;
	@Parameter(names={"--trim"},description="trim long string to this length. <1 = do not trim.")
	private int trim_long_string_length=50;
	@Parameter(names={"-noclip","--noclip","--no-clipping"},description="hide clipped bases")
	private boolean hide_clipping = false;
	@Parameter(names={"-cN","--collapse-N"},description="collapse cigar operator 'N'")
	private boolean collapse_N_operator = false;
	@Parameter(names={"-cD","--collapse-ND"},description="collapse cigar operator 'D'")
	private boolean collapse_D_operator = false;
	@Parameter(names={"-nT","--no-attributes"},description="hide attributes table")
	private boolean hide_attribute_table = false;
	@Parameter(names={"-nA","--no-alignment"},description="hide alignment")
	private boolean hide_alignment = false;
	@Parameter(names={"-nS","--no-suppl"},description="hide supplementary alignements")
	private boolean hide_supplementary_align = false;
	@Parameter(names={"-nP","--no-program-record"},description="hide program record")
	private boolean hide_program_record = false;
	@Parameter(names={"-nR","--no-read-group"},description="hide read group")
	private boolean hide_read_group = false;
	@Parameter(names={"-nC","--no-cigar"},description="hide cigar string")
	private boolean hide_cigar_string = false;


	
	private static char HISTOGRAM_CHARS[] = new char[]{'\u2581', '\u2582', '\u2583', '\u2584', '\u2585', '\u2586', '\u2587', '\u2588'};
    private static final Pattern SEMICOLON_PAT = Pattern.compile("[;]");
    private static final Pattern COMMA_PAT = Pattern.compile("[,]");
	private ReferenceGenome referenceGenome = null;

	
	private static class Base
		{
		char readbase='\0';
		char readqual='\0';
		int  readpos=-1;
		char refbase='\0';
		int  refpos=-1;
		CigarOperator cigaroperator = null;
		}

	private final Function<Boolean, String> isNegativeStrandToString = NEGATIVE ->
		(this.disable_unicode?(NEGATIVE?"<--":"-->"):(NEGATIVE?"\u2190":"\u2192"));
	
	public class PrettySAMWriter implements SAMFileWriter
		{
		private final NumberFormat fmt = new DecimalFormat("#,###");
		private final PrintWriter pw ;
		private SAMFileHeader header = null;
		private SAMSequenceDictionary samDict=null;
		private SAMSequenceDictionary faidxDict=null;
		private long nLine=0;
		private ReferenceGenome referenceGenome=null;
		private ReferenceContig genomicSequence=null;
		private final Map<String,String> tags = new HashMap<>();
		private final Map<String,String> readgroupAtt2def = new HashMap<>();
		private ContigNameConverter contigNameConverter=null;
		
		public PrettySAMWriter(PrintWriter pw) {
			this.pw = pw;
			
			// READ GROUP ATTRIBUTE
			this.readgroupAtt2def.put("ID","Read group id");
			this.readgroupAtt2def.put("CN","Sequencing Center");
			this.readgroupAtt2def.put("DS","Description");
			this.readgroupAtt2def.put("DT","Date run produced");
			this.readgroupAtt2def.put("FO","Flow order");
			this.readgroupAtt2def.put("KS","Key sequence");
			this.readgroupAtt2def.put("LB","Library");
			this.readgroupAtt2def.put("PG","Program group");
			this.readgroupAtt2def.put("PI","Predicted median insert size");
			this.readgroupAtt2def.put("PL","Platform");
			this.readgroupAtt2def.put("PM","platform model");
			this.readgroupAtt2def.put("PU","Platform unit");
			this.readgroupAtt2def.put("SM","Sample");
			
			// ATTRIBUTE
			tags.put("AM","The smallest template-independent mapping quality of segments in the rest");
			tags.put("AS","Alignment score generated by aligner");
			tags.put("BC","Barcode sequence identifying the sample");
			tags.put("BQ","Offset to base alignment quality (BAQ)");
			tags.put("BZ","Phred quality of the unique molecular barcode bases in the {\tt OX} tag");
			tags.put("CC","Reference name of the next hit");
			tags.put("CG","BAM only: CIGAR in BAM's binary encoding if (and only if) it consists of >65535 operators");
			tags.put("CM","Edit distance between the color sequence and the color reference.");
			tags.put("CO","Free-text comments");
			tags.put("CP","Leftmost coordinate of the next hit");
			tags.put("CQ","Color read base qualities");
			tags.put("CS","Color read sequence");
			tags.put("CT","Complete read annotation tag, used for consensus annotation dummy features");
			tags.put("E2","The 2nd most likely base calls");
			tags.put("FI","The index of segment in the template");
			tags.put("FS","Segment suffix");
			tags.put("FZ","Flow signal intensities");
			tags.put("GC","Reserved for backwards compatibility reasons");
			tags.put("GQ","Reserved for backwards compatibility ttreasons");
			tags.put("GS","Reserved for backwards compatibility reasons");
			tags.put("H0","Number of perfect hits");
			tags.put("H1","Number of 1-difference hits (see also NM)");
			tags.put("H2","Number of 2-difference hits");
			tags.put("HI","Query hit index");
			tags.put("IH","Number of stored alignments in SAM that contains the query in the current record");
			tags.put("LB","Library");
			tags.put("MC","CIGAR string for mate/next segment");
			tags.put("MD","String for mismatching positions");
			tags.put("MF","Reserved for backwards compatibility reasons");
			tags.put("MI","Molecular identifier; a string that uniquely identifies the molecule from which the record was derived");
			tags.put("MQ","Mapping quality of the mate/next segment");
			tags.put("NH","Number of reported alignments that contains the query in the current record");
			tags.put("NM","Edit distance to the reference");
			tags.put("OC","Original CIGAR");
			tags.put("OP","Original mapping position");
			tags.put("OQ","Original base quality");
			tags.put("OX","Original unique molecular barcode bases");
			tags.put("PG","Program");
			tags.put("PQ","Phred likelihood of the template");
			tags.put("PT","Read annotations for parts of the padded read sequence");
			tags.put("PU","Platform unit");
			tags.put("Q2","Phred quality of the mate/next segment sequence in the R2 tag");
			tags.put("QT","Phred quality of the sample-barcode sequence in the BC or RT tag");
			tags.put("QX","Quality score of the unique molecular identifier in the RX tag");
			tags.put("R2","Sequence of the mate/next segment in the template");
			tags.put("RG","Read group");
			tags.put("RT","Barcode sequence (deprecated; use BC instead)");
			tags.put("RX","Sequence bases of the (possibly corrected) unique molecular identifier");
			tags.put("SA","Other canonical alignments in a chimeric alignment");
			tags.put("SM","Template-independent mapping quality");
			tags.put("SQ","Reserved for backwards compatibility reasons");
			tags.put("S2","Reserved for backwards compatibility reasons");
			tags.put("TC","The number of segments in the template");
			tags.put("U2","Phred prob. of the 2nd call being wrong conditional on the best being wrong");
			tags.put("UQ","Phred likelihood of the segment, conditional on the mapping being correct");
			//tags.put("X?","Reserved for end users");
			//tags.put("Y?","Reserved for end users");
			//tags.put("Z?","Reserved for end users");			
			}
		
		/** returns SAM attribute definition. Never null */
		private String getTagDescription(final String attName) {
			if(attName.equals(ColorUtils.YC_TAG)) {
				return "IGV Color tag";
				}
			if(attName.startsWith("X") ||
				attName.startsWith("Y") ||
				attName.startsWith("Z"))
				{
				return "Reserved for end users";
				}
			final String value = this.tags.get(attName);
			return StringUtil.isBlank(value)?"Unknown":value;
		}
		

		
		
		private char getReferenceAt(final String contig,int refpos) {
			if(this.genomicSequence==null || !this.genomicSequence.hasName(contig)) {
				if(PrettySam.this.referenceGenome==null || this.contigNameConverter==null) return 'N';
				final String normContig = this.contigNameConverter.apply(contig);
				if(normContig==null) return 'N';
				this.genomicSequence = PrettySam.this.referenceGenome.getContig(normContig);
				if(this.genomicSequence==null) return 'N';
				}
			if((refpos-1)<0 || (refpos-1)>=this.genomicSequence.length())return 'N';
			return this.genomicSequence.charAt(refpos-1);
			}
		
		@Override
		public void setProgressLogger(final ProgressLoggerInterface progress) {
			}
		@Override
		public SAMFileHeader getFileHeader() {
			return header;
		}	
		
		public void writeHeader(final SAMFileHeader header) {
			this.header = header;
			this.samDict = this.header.getSequenceDictionary();
			
			if(PrettySam.this.referenceGenome!=null) {
				this.faidxDict = PrettySam.this.referenceGenome.getDictionary();
				}

			
			if(this.samDict!=null && this.faidxDict!=null) {
				this.contigNameConverter  = ContigNameConverter.fromDictionaries(this.samDict, this.faidxDict);
				this.contigNameConverter.setOnNotFound(OnNotFound.SKIP);
				}
			pw.flush();
			}
		
		private void label(int cols,final String s) { pw.printf("%"+cols+"s : ",s); }
		private String trimToLen(final Object o) {
			final String s = String.valueOf(o);
			if(PrettySam.this.trim_long_string_length<1) return s;
			return s.length()-3 > PrettySam.this.trim_long_string_length?
					s.substring(0, PrettySam.this.trim_long_string_length)
					+"...":s;
			}
		@Override
		public void addAlignment(final SAMRecord rec) {
			final int margin1=19;
			final int margin2=margin1+5;
			
			++this.nLine;
			if(this.nLine>1) pw.println();
			pw.println(">>>>> "+nLine);
			label(margin1,"Read-Name");pw.println(rec.getReadName());
			
			 new IlluminaReadName.Parser().apply(rec.getReadName()).
			 	ifPresent(ilmn->{
					label(margin2,"Instrument");pw.println(ilmn.getInstrument());
					label(margin2,"Run");pw.println(ilmn.getRunId());
					label(margin2,"FlowCell");pw.println(ilmn.getFlowCell());
					label(margin2,"Lane");pw.println(ilmn.getLane());
					label(margin2,"Tile");pw.println(ilmn.getTile());
					label(margin2,"X");pw.println(ilmn.getX());
					label(margin2,"Y");pw.println(ilmn.getY());
					});
			label(margin1,"Flag");pw.println(rec.getFlags());
			for(final SAMFlag flg:SAMFlag.values())
				{
				if(!flg.isSet(rec.getFlags())) continue;
				label(margin2,String.valueOf(flg.intValue()));pw.println(flg.getLabel());
				}
			if(!rec.getReadUnmappedFlag()) {
				label(margin1,"MAPQ");
				pw.print(rec.getMappingQuality());
				if(rec.getMappingQuality()==SAMRecord.UNKNOWN_MAPPING_QUALITY)
					{
					pw.print(" (unknown)");
					}
				pw.println();
				
				label(margin1,"Contig");pw.println(rec.getReferenceName()+"  (index:"+rec.getReferenceIndex()+")");
				if(rec.getUnclippedStart()!=rec.getStart())
					{
					label(margin1,"Unclipped-Start");pw.println(this.fmt.format(rec.getUnclippedStart()));
					}
				label(margin1,"Start");pw.println(this.fmt.format(rec.getAlignmentStart()));
				label(margin1,"End");pw.println(this.fmt.format(rec.getAlignmentEnd()));
				if(rec.getUnclippedEnd()!=rec.getEnd())
					{
					label(margin1,"Unclipped-End");pw.println(this.fmt.format(rec.getUnclippedEnd()));
					}
				label(margin1,"Strand");pw.println(isNegativeStrandToString.apply(rec.getReadNegativeStrandFlag()));
				}
			if(rec.getReadPairedFlag() && !rec.getMateUnmappedFlag() && rec.getMateReferenceIndex()>=0)
				{
				if(!rec.getReadUnmappedFlag() && rec.getInferredInsertSize()!=0)
					{
					label(margin1,"Insert-Size");pw.println(this.fmt.format(rec.getInferredInsertSize()));
					}
				label(margin1,"Mate-Contig");pw.println(rec.getMateReferenceName()+"  (index:"+rec.getMateReferenceIndex()+")");
				label(margin1,"Mate-Start");pw.println(this.fmt.format(rec.getMateAlignmentStart()));
				label(margin1,"Mate-Strand");pw.println(isNegativeStrandToString.apply(rec.getMateNegativeStrandFlag()));
				}
			if(!PrettySam.this.hide_program_record &&
				this.header!=null && rec.hasAttribute(SAMProgramRecord.PROGRAM_GROUP_ID_TAG)) {
				final String pgId = rec.getStringAttribute(SAMProgramRecord.PROGRAM_GROUP_ID_TAG);
				final SAMProgramRecord programrec = StringUtil.isBlank(pgId)?null:this.header.getProgramRecord(pgId);
				if(programrec!=null) {
					label(margin1,"Program Group");
					pw.println();
					for(Map.Entry<String,String> entry:programrec.getAttributes())
						{
						label(margin2,entry.getKey());
						pw.printf("%10s\n",entry.getValue());
						}					
					}		
				}
			if(!PrettySam.this.hide_read_group) {
				final SAMReadGroupRecord grouprec = rec.getReadGroup();
				if(grouprec!=null )
					{
					label(margin1,"Read Group");
					pw.println();
					label(margin2,"ID");
					pw.printf("%10s\n",grouprec.getId());
					for(Map.Entry<String,String> entry:grouprec.getAttributes())
						{
						final String def = this.readgroupAtt2def.get(entry.getKey());
						label(margin2,entry.getKey());
						pw.printf("%10s",entry.getValue());
						if(!StringUtil.isBlank(def)) {
							pw.print("    \""+def+"\"");
							}
						pw.println();
						}
					}
				}
			
			label(margin1,"Read-Length");
			pw.println(this.fmt.format(rec.getReadLength()));
			
			final Cigar cigar=rec.getCigar();
			if(!PrettySam.this.hide_cigar_string) {
				if(cigar!=null && !cigar.isEmpty())
					{
					label(margin1,"Cigar");
					if(rec.getCigarString().length()<=50)
						{
						pw.println(rec.getCigarString()+" (N="+cigar.numCigarElements()+")");
						}
					else
						{
						pw.println("N="+cigar.numCigarElements());
						int x=0;
						while(x<cigar.numCigarElements())
							{
							for(int y=0;y< margin2;++y) pw.print(" ");
							for(int y=0;y< 30 && x<cigar.numCigarElements();++y)
								{
								final CigarElement ce = cigar.getCigarElement(x);
								pw.print(String.valueOf(+ce.getLength())+ce.getOperator().name());
								++x;
								}
							pw.println();
							}
						}
					}
				}
			
			final List<Base> align = new ArrayList<>();
			final String bases = rec.getReadString();
			final String quals = rec.getBaseQualityString();
			
			if(PrettySam.this.hide_alignment)
				{
				//nothing
				}
			else if(rec.getReadUnmappedFlag() || cigar==null || cigar.isEmpty())
				{
				for(int i=0;i< bases.length();i++)
					{
					final Base b=new Base();
					b.readpos=i;
					b.refbase = ' ';
					b.refpos=-1;
					b.cigaroperator = CigarOperator.P;
					b.readbase = (bases!=null && i>=0 && i<bases.length()?bases.charAt(i):'*');
					b.readqual = (quals!=null && i>=0 && i<quals.length()?quals.charAt(i):'?');
					align.add(b);
					}
				}
			else
				{
				int refpos=rec.getUnclippedStart();
				int readpos=0;
				if(cigar.numCigarElements()>1 && cigar.getCigarElement(0).getOperator()==CigarOperator.H)
					{
					readpos = rec.getUnclippedStart()-rec.getAlignmentStart();//WILL be negative !
					}
				
				
				final Function<Integer, Character> pos2base= (i)->(bases!=null && i>=0 && i<bases.length()?bases.charAt(i):'*');
				final Function<Integer, Character> pos2qual= (i)->(quals!=null && i>=0 && i<quals.length()?quals.charAt(i):'\0');
				final Function<Integer, Character> pos2ref= (i)->getReferenceAt(rec.getContig(),i);
				
				
				for(final CigarElement ce:cigar)
					{
					final CigarOperator op = ce.getOperator();
					switch(op)
						{
						case P: break;
						case D: case N:
							{
							for(int i=0;i< ce.getLength();++i)
								{
								final Base b=new Base();
								b.cigaroperator = op;
								b.refpos  = refpos;
								//b.refbase = pos2ref.apply(refpos); <- later because time consumming
								b.readbase = '-';
								b.readqual = '\0';
								b.readpos = -1;
								if(i>0 && (
										(PrettySam.this.collapse_D_operator && op.equals(CigarOperator.D)) ||
										(PrettySam.this.collapse_N_operator && op.equals(CigarOperator.N))
										))
									{
									//ignore
									}
								else
									{
									b.refbase = pos2ref.apply(refpos);
									align.add(b);
									}
								refpos++;
								}
							break;
							}
						case X:case EQ:case M:case S:case H:case I:
							{
							for(int i=0;i< ce.getLength();++i)
								{
								final Base b=new Base();
								b.refpos=refpos;
								b.cigaroperator = op;
								b.readbase = pos2base.apply(readpos);
								b.readqual = pos2qual.apply(readpos);
								b.readpos=readpos;
								
								readpos++;//ok with 'H', because negative from beginning *
								if(op.equals(CigarOperator.I))
									{
									b.refpos = -1;
									b.refbase  = '-';
									}
								else
									{
									b.refpos= refpos;
									b.refbase  = pos2ref.apply(refpos);
									refpos++;
									}
								
								if(op.isClipping() && PrettySam.this.hide_clipping)
									{
									// nothing
									}
								else
									{
									align.add(b);
									}
								}
							break;
							}
						}
					}
				}
			if(PrettySam.this.hide_alignment)
				{
				//nothing
				}
			else if(!align.isEmpty())
				{
				label(margin1,"Sequence");
				pw.println();
				int x=0;
				final int FASTA_LEN=60;
				while(x<align.size())
					{
					for(int side=0;side<6;++side)
						{
						switch(side)
							{
							case 0: label(margin2,"Read ("+ this.fmt.format(align.stream().
									skip(x).
									mapToInt(B->B.readpos).
									filter(X -> X>=0).
									findFirst().orElse(-1))+")");
									break;
							case 4: label(margin2,"Qual");break;
							case 2:
								if(cigar==null || cigar.isEmpty()|| PrettySam.this.referenceGenome==null) continue;
								label(margin2,"Ref ("+this.fmt.format(align.stream().
										skip(x).
										mapToInt(B->B.refpos).
										filter(X -> X>0).
										findFirst().orElse(-1))+")");
								break;
							case 3:
								if(cigar==null || cigar.isEmpty()) continue;
								label(margin2,"Cigar-Operator");break;
							case 1:
								if(cigar==null || cigar.isEmpty() || PrettySam.this.referenceGenome==null) continue;
								label(margin2,"Middle");break;
							case 5: label(margin2,"Ref-Pos");break;
							default:break;
							}
						for(int y=0;y<FASTA_LEN && x+y<align.size();++y)
							{
							final Base base = align.get(x+y);
							if(y!=0 && y%10==0) pw.print(" ");
							final char c;
							switch(side)
								{
								case 0:{
									if(base.cigaroperator==CigarOperator.N || base.cigaroperator==CigarOperator.D )
										{
										c='-';
										}
									else
										{
										c =  base.readbase;
										}
									break;
									}
								case 4: {
									if(base.cigaroperator==CigarOperator.N || base.cigaroperator==CigarOperator.D || base.readqual=='\0')
										{
										c=' ';
										}
									else if(PrettySam.this.disable_unicode)
										{
										c= base.readqual;
										}
									else
										{
										final double GOOD_QUAL=80.0;//should be (255-33) but let's say this is already very good
										double q= (((int)base.readqual-33.00)/GOOD_QUAL);
										q=Math.max(0.0,Math.min(q,1.0));
										c=HISTOGRAM_CHARS[(int)(q*HISTOGRAM_CHARS.length)];
										}
									break;
									}
								case 2: {
									if(base.cigaroperator==CigarOperator.INSERTION)
										{
										c='-';
										}
									else
										{
										c =  base.refbase;
										}
									break;
									}
								case 3: c = base.cigaroperator.name().charAt(0);
									break;
								case 1:
									c= base.cigaroperator.isAlignment()?
											(
											Character.toUpperCase(base.refbase)==Character.toUpperCase(base.readbase)?'|':' ')
											: ' '
											;
									break;
								case 5: {
									if(y%10==0 && base.refpos>0)
										{
										String s=String.valueOf(base.refpos);
										while(s.length()<10) s+=" ";
										pw.print(s);
										y+=(s.length()-1);/* -1 pour boucle for */
										}
									else
										{
										pw.print(" ");
										}
									continue;
									}
								default : c='?'; break;
								}
							pw.print(c);
							}
						pw.println();
						
						}
					x+=FASTA_LEN;
					pw.println();
					}
				}
			if(!PrettySam.this.hide_attribute_table && 
				!rec.getAttributes().isEmpty()
				) {
				label(margin1,"Tags");
				pw.println();
				
				for(final SAMRecord.SAMTagAndValue tav: rec.getAttributes())
					{
					if(tav.tag.equals("SA")) continue;
					if(tav.tag.equals("RG")) continue;
					if(tav.tag.equals(SAMProgramRecord.PROGRAM_GROUP_ID_TAG)) continue;
					label(margin2,tav.tag);
					pw.print(" ");
					if(tav.value!=null && tav.value.getClass().isArray())
						{
						pw.print(" array(todo)");
						}
					else
						{
						pw.print(trimToLen(tav.value));
						}
					pw.print("   \"");
					pw.print(getTagDescription(tav.tag));
					pw.println("\"");
					}
				}
			
			if(!PrettySam.this.hide_supplementary_align && rec.hasAttribute("SA"))
				{
				label(margin1,"Suppl. Alignments");
				pw.println();
	
				final List<List<String>> salist = new ArrayList<>();
		        final String semiColonStrs[] = SEMICOLON_PAT.split(rec.getStringAttribute("SA"));
		        for (int i = 0; i < semiColonStrs.length; ++i) {
		            final String semiColonStr = semiColonStrs[i];
		            if (semiColonStr.isEmpty()) continue;
		            final String commaStrs[] = COMMA_PAT.split(semiColonStr);
		            if (commaStrs.length != 6) continue;
		            // cigar too long ?
		            if(commaStrs[3].length()>53) commaStrs[3]=trimToLen(commaStrs[3]);
		            salist.add(Arrays.asList(commaStrs));
		        	}
		        final String labels[]=new String[]{"CONTIG","POS","STRAND","CIGAR","QUAL","NM"};
		        final int collen[] = new int[labels.length];
		        for(int i=0;i<labels.length;++i)
		        	{
		        	final int colidx=i;
		        	collen[i] = Math.max(
		        			salist.stream().mapToInt(L->L.get(colidx).length()).max().getAsInt(),
		        			labels[i].length());
		        	}
		        int y=-1;
		        while(y<salist.size())
			        {
		        	for(int x=0;x<margin2;++x) pw.print(" ");
					for(int i=0;i<labels.length;i++)
						{
						String s = (y==-1?labels[i]:salist.get(y).get(i));
						while(s.length()<collen[i]) s+=" ";
						if(i>0) pw.print(" | ");
						pw.print(s);
						}
					++y;
					pw.println();
			        }
				}	
			pw.println("<<<<< "+nLine);
			pw.flush();
			}
		
		@Override
		public void close() {
			pw.close();
			CloserUtil.close(this.referenceGenome);
			this.genomicSequence=null;
			}
		}
	@Override
	public int doWork(final List<String> args) {
		SamReader r = null;
		PrettySAMWriter out = null;
		CloseableIterator<SAMRecord> iter = null;
		try 
			{
			r= super.openSamReader(oneFileOrNull(args));
			if(this.referenceUri!=null)
				{
				this.referenceGenome  = new ReferenceGenomeFactory().
						open(this.referenceUri);
				}
			else
				{
				this.referenceGenome = null;
				}			
			out = new PrettySAMWriter(super.openFileOrStdoutAsPrintWriter(this.outputFile));
			out.writeHeader(r.getFileHeader());
			iter = r.iterator();
			while(iter.hasNext())
				{
				out.addAlignment(iter.next());
				}
			out.close();out=null;
			return 0;
			}
		catch(final Throwable err)
			{
			LOG.error(err);
			return -1;
			}
		finally
			{
			CloserUtil.close(iter);
			CloserUtil.close(r);
			CloserUtil.close(out);
			}
		}
	
	public static void main(final String[] args) {
		new PrettySam().instanceMainWithExit(args);
	}

}