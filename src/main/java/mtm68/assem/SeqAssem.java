package mtm68.assem;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import mtm68.util.ArrayUtils;

public class SeqAssem extends Assem {

	private List<Assem> assems;

	public SeqAssem(List<Assem> assems) {
		this.assems = flattenSeqs(assems);
	}
	
	public SeqAssem(Assem...assems) {
		this(ArrayUtils.elems(assems));
	}
	
	private List<Assem> flattenSeqs(List<Assem> assems) {
		List<Assem> result = new ArrayList<>();
		for(Assem assem : assems) {
			if(assem instanceof SeqAssem) {
				result.addAll(flattenSeqs(((SeqAssem)assem).getAssems()));
			}
			else if (assem != null) {
				result.add(assem);
			}
		}
		return result;
	}
	
	public List<Assem> getAssems() {
		return assems;
	}

	public void appendAssems(List<Assem> assems) {
		assems.addAll(flattenSeqs(assems));
	}
	
	public void prependAssem(Assem assem) {
		assems.add(0, assem);
	}
	
	public void setAssems(List<Assem> assems) {
		this.assems = assems;
	}

	@Override
	public String toString() {
		return assems.stream()
				.map(Assem::toString)
				.collect(Collectors.joining("\n"));
	}
	
	@Override
	public List<ReplaceableReg> getReplaceableRegs() {
		return assems.stream()
					.map(Assem::getReplaceableRegs)
					.flatMap(List::stream)
					.collect(Collectors.toList());
	}
}
