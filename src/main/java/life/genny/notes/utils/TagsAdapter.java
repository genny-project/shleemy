package life.genny.notes.utils;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.bind.adapter.JsonbAdapter;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.notes.models.Tag;

@RegisterForReflection
public class TagsAdapter implements JsonbAdapter<Set<Tag>, JsonObject> {


	@Override
	public JsonObject adaptToJson(Set<Tag> obj) throws Exception {
		String tagsCommaSeparated = obj.stream()
                .map(t -> {
                	String ret = t.getName()+":"+t.getValue();
                	return ret;
                })
                .collect(Collectors.joining(","));
		return Json.createObjectBuilder()
	              .add("tags", tagsCommaSeparated)
	              .build();
	}

	@Override
	public Set<Tag> adaptFromJson(JsonObject obj) throws Exception {
		String tagsCommaSeparated = obj.getString("tags");
		String[] tagValues = tagsCommaSeparated.split(",");
		Set<Tag> tags = new HashSet<>();
		for (int i=0;i<tagValues.length;i++) {
			String tagValueStr = tagValues[i];
			String[] tagValueStrArray = tagValueStr.split(":");
			String tagStr = tagValueStrArray[0];
			Integer value = Integer.parseInt(tagValueStrArray[1]);
			Tag tag = new Tag(tagStr,value);
			tags.add(tag);
		}
		
	return tags;
	}
	

}