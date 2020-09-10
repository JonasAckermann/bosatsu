# Do not edit. bazel-deps autogenerates this file from dependencies.yaml.
def _jar_artifact_impl(ctx):
    jar_name = "%s.jar" % ctx.name
    ctx.download(
        output=ctx.path("jar/%s" % jar_name),
        url=ctx.attr.urls,
        sha256=ctx.attr.sha256,
        executable=False
    )
    src_name="%s-sources.jar" % ctx.name
    srcjar_attr=""
    has_sources = len(ctx.attr.src_urls) != 0
    if has_sources:
        ctx.download(
            output=ctx.path("jar/%s" % src_name),
            url=ctx.attr.src_urls,
            sha256=ctx.attr.src_sha256,
            executable=False
        )
        srcjar_attr ='\n    srcjar = ":%s",' % src_name

    build_file_contents = """
package(default_visibility = ['//visibility:public'])
java_import(
    name = 'jar',
    tags = ['maven_coordinates={artifact}'],
    jars = ['{jar_name}'],{srcjar_attr}
)
filegroup(
    name = 'file',
    srcs = [
        '{jar_name}',
        '{src_name}'
    ],
    visibility = ['//visibility:public']
)\n""".format(artifact = ctx.attr.artifact, jar_name = jar_name, src_name = src_name, srcjar_attr = srcjar_attr)
    ctx.file(ctx.path("jar/BUILD"), build_file_contents, False)
    return None

jar_artifact = repository_rule(
    attrs = {
        "artifact": attr.string(mandatory = True),
        "sha256": attr.string(mandatory = True),
        "urls": attr.string_list(mandatory = True),
        "src_sha256": attr.string(mandatory = False, default=""),
        "src_urls": attr.string_list(mandatory = False, default=[]),
    },
    implementation = _jar_artifact_impl
)

def jar_artifact_callback(hash):
    src_urls = []
    src_sha256 = ""
    source=hash.get("source", None)
    if source != None:
        src_urls = [source["url"]]
        src_sha256 = source["sha256"]
    jar_artifact(
        artifact = hash["artifact"],
        name = hash["name"],
        urls = [hash["url"]],
        sha256 = hash["sha256"],
        src_urls = src_urls,
        src_sha256 = src_sha256
    )
    native.bind(name = hash["bind"], actual = hash["actual"])


def list_dependencies():
    return [
    {"artifact": "com.lihaoyi:fastparse-utils_2.11:1.0.0", "lang": "scala", "sha1": "98716ae2093a51449f41485009ce1bb1cefd3336", "sha256": "90bc5d8979d6b1b95f636d5d5751033884a5cb500cf812b152ab6fe5c972e7bf", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/com/lihaoyi/fastparse-utils_2.11/1.0.0/fastparse-utils_2.11-1.0.0.jar", "source": {"sha1": "e2735bfc5fbf85a21f7335ac672e27b4ec55358c", "sha256": "6a092add91bab7bf903cf74d18a8d7c3023e832fb00ac0cf176db7b35dfd9e48", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/com/lihaoyi/fastparse-utils_2.11/1.0.0/fastparse-utils_2.11-1.0.0-sources.jar"} , "name": "com_lihaoyi_fastparse_utils_2_11", "actual": "@com_lihaoyi_fastparse_utils_2_11//jar:file", "bind": "jar/com/lihaoyi/fastparse_utils_2_11"},
    {"artifact": "com.lihaoyi:fastparse_2.11:1.0.0", "lang": "scala", "sha1": "334cc8841a7f72a16c258252232fd1db8c0e9791", "sha256": "1b6d9fc75ca8a62abe0dd7a71e62aa445f2d3198c86aab5088e1f90a96ade30b", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/com/lihaoyi/fastparse_2.11/1.0.0/fastparse_2.11-1.0.0.jar", "source": {"sha1": "32f0ce22bbe0407737ac5346952a3556062400cf", "sha256": "f8792af99935264e1d23d882012b6d89aacbe74d820f63af866b5b162cb5d034", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/com/lihaoyi/fastparse_2.11/1.0.0/fastparse_2.11-1.0.0-sources.jar"} , "name": "com_lihaoyi_fastparse_2_11", "actual": "@com_lihaoyi_fastparse_2_11//jar:file", "bind": "jar/com/lihaoyi/fastparse_2_11"},
    {"artifact": "com.lihaoyi:sourcecode_2.11:0.1.4", "lang": "scala", "sha1": "78369535832ebb91fb1c4054c5662b2c9a0d2c88", "sha256": "e0edffec93ddef29c40b7c65580960062a3fa9d781eddb8c64e19e707c4a8e7c", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/com/lihaoyi/sourcecode_2.11/0.1.4/sourcecode_2.11-0.1.4.jar", "source": {"sha1": "c910950e1170659ba1671b24c2c4f8f2b174b336", "sha256": "b6a282beaca27092692197c017cbd349dccf526100af1bbd7f78cf462219f7f9", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/com/lihaoyi/sourcecode_2.11/0.1.4/sourcecode_2.11-0.1.4-sources.jar"} , "name": "com_lihaoyi_sourcecode_2_11", "actual": "@com_lihaoyi_sourcecode_2_11//jar:file", "bind": "jar/com/lihaoyi/sourcecode_2_11"},
    {"artifact": "com.monovore:decline_2.11:1.0.0", "lang": "scala", "sha1": "c04debe43b9ffe49a8d3eafe5ae93c7a714158b4", "sha256": "cf34e7b2b9bd2eb31d06869c307ef389124011d9d83e8fe77f1b2ecba890169d", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/com/monovore/decline_2.11/1.0.0/decline_2.11-1.0.0.jar", "source": {"sha1": "0c351dc322108b2691140ec3c1bfd1324cca54a9", "sha256": "42acaa1e3b596ada20ba25f0d11999830746f843c5c7d9cf0e05ad45d1bb00cf", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/com/monovore/decline_2.11/1.0.0/decline_2.11-1.0.0-sources.jar"} , "name": "com_monovore_decline_2_11", "actual": "@com_monovore_decline_2_11//jar:file", "bind": "jar/com/monovore/decline_2_11"},
    {"artifact": "com.stripe:dagon-core_2.11:0.2.2", "lang": "scala", "sha1": "03d2ca9d27bbb3f494b2eb9414e121ede364f50a", "sha256": "d3ae2014a607859861b273046717b6a283dc140d7c3e945fc3b4eca812ea3fc4", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/com/stripe/dagon-core_2.11/0.2.2/dagon-core_2.11-0.2.2.jar", "source": {"sha1": "e622cf39dc59644d2ab0e7bc00a5fbcc5ef4db5e", "sha256": "1f6e315f39ec5090e3fdd8a24f3f82740032e377d249eea9f63b216027b46d83", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/com/stripe/dagon-core_2.11/0.2.2/dagon-core_2.11-0.2.2-sources.jar"} , "name": "com_stripe_dagon_core_2_11", "actual": "@com_stripe_dagon_core_2_11//jar:file", "bind": "jar/com/stripe/dagon_core_2_11"},
    {"artifact": "org.bykn:fastparse-cats-core_2.11:0.1.0", "lang": "scala", "sha1": "b68edaceff33482939a73517234781cc58cfe2ed", "sha256": "f3e9e993b090992faf2ab0230e2c298ec5d531d5402e790523f17dd7d513a3f4", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/bykn/fastparse-cats-core_2.11/0.1.0/fastparse-cats-core_2.11-0.1.0.jar", "source": {"sha1": "cad7179d59a0c2a867b21c750f8fca5580d9f496", "sha256": "6ff63a4381db24b7c483c83a4f8b9975b14ac4c360c9510b375d84e9aa9aa0db", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/bykn/fastparse-cats-core_2.11/0.1.0/fastparse-cats-core_2.11-0.1.0-sources.jar"} , "name": "org_bykn_fastparse_cats_core_2_11", "actual": "@org_bykn_fastparse_cats_core_2_11//jar:file", "bind": "jar/org/bykn/fastparse_cats_core_2_11"},
    {"artifact": "org.scala-sbt:test-interface:1.0", "lang": "java", "sha1": "0a3f14d010c4cb32071f863d97291df31603b521", "sha256": "15f70b38bb95f3002fec9aea54030f19bb4ecfbad64c67424b5e5fea09cd749e", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/scala-sbt/test-interface/1.0/test-interface-1.0.jar", "source": {"sha1": "d44b23e9e3419ad0e00b91bba764a48d43075000", "sha256": "c314491c9df4f0bd9dd125ef1d51228d70bd466ee57848df1cd1b96aea18a5ad", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/scala-sbt/test-interface/1.0/test-interface-1.0-sources.jar"} , "name": "org_scala_sbt_test_interface", "actual": "@org_scala_sbt_test_interface//jar", "bind": "jar/org/scala_sbt/test_interface"},
    {"artifact": "org.scalacheck:scalacheck_2.11:1.13.5", "lang": "scala", "sha1": "4800dfc0e73bd9af55a89ba7c8ec44c46b6f034f", "sha256": "7e55593585376e799b5c93561ee97b8c9e2a6e479205377e7bb9a77d5bd1f854", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/scalacheck/scalacheck_2.11/1.13.5/scalacheck_2.11-1.13.5.jar", "source": {"sha1": "0ed27a94e5d447b9a23cc169eb424092ed8d259a", "sha256": "d7ab366a782c957ba116aa47e7a86d4e74850c351875b0a347a235a1fe22c269", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/scalacheck/scalacheck_2.11/1.13.5/scalacheck_2.11-1.13.5-sources.jar"} , "name": "org_scalacheck_scalacheck_2_11", "actual": "@org_scalacheck_scalacheck_2_11//jar:file", "bind": "jar/org/scalacheck/scalacheck_2_11"},
    {"artifact": "org.scalactic:scalactic_2.11:3.0.1", "lang": "scala", "sha1": "3c444d143879dc172fa555cea08fd0de6fa2f34f", "sha256": "d5586d4aa060aebbf0ccb85be62208ca85ccc8c4220a342c22783adb04b1ded1", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/scalactic/scalactic_2.11/3.0.1/scalactic_2.11-3.0.1.jar", "source": {"sha1": "3de4d4b57f2cfc20c916a9444dd713235bf626ac", "sha256": "119b51c8a98623d259395d5688e814d3b46d4a8f5da9a9f0842ff988f8f03a1c", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/scalactic/scalactic_2.11/3.0.1/scalactic_2.11-3.0.1-sources.jar"} , "name": "org_scalactic_scalactic_2_11", "actual": "@org_scalactic_scalactic_2_11//jar:file", "bind": "jar/org/scalactic/scalactic_2_11"},
# duplicates in org.scalatest:scalatest_2.11 fixed to 3.0.1
# - org.bykn:fastparse-cats-core_2.11:0.1.0 wanted version 3.0.5
# - org.typelevel:cats-testkit_2.11:1.1.0 wanted version 3.0.3
    {"artifact": "org.scalatest:scalatest_2.11:3.0.1", "lang": "scala", "sha1": "40a1842e7f0b915d87de1cb69f9c6962a65ee1fd", "sha256": "3788679b5c8762997b819989e5ec12847df3fa8dcb9d4a787c63188bd953ae2a", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/scalatest/scalatest_2.11/3.0.1/scalatest_2.11-3.0.1.jar", "source": {"sha1": "a419220815f884a36f461e6b05fc9303976ed8cb", "sha256": "a7532b9f0963060ce292b3d6705a7efa238960d38af8a1cc7da6fb72f6d54982", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/scalatest/scalatest_2.11/3.0.1/scalatest_2.11-3.0.1-sources.jar"} , "name": "org_scalatest_scalatest_2_11", "actual": "@org_scalatest_scalatest_2_11//jar:file", "bind": "jar/org/scalatest/scalatest_2_11"},
    {"artifact": "org.spire-math:kind-projector_2.11:0.9.4", "lang": "scala", "sha1": "d8872b2c067d3c9b57bf4809d0d0ca77ed9f5435", "sha256": "081f2c09b886b634f8613739bfb8d64d8d52020e5fe92a918fd55b70f4a27897", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/spire-math/kind-projector_2.11/0.9.4/kind-projector_2.11-0.9.4.jar", "source": {"sha1": "8ca363a721eb00f3812c7f1b3d56f9776e653ba6", "sha256": "7dc6a86e2aa26d38f80759bf117535c3114e906b0ba383ea47c1f90fc876a5c6", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/spire-math/kind-projector_2.11/0.9.4/kind-projector_2.11-0.9.4-sources.jar"} , "name": "org_spire_math_kind_projector_2_11", "actual": "@org_spire_math_kind_projector_2_11//jar:file", "bind": "jar/org/spire_math/kind_projector_2_11"},
    {"artifact": "org.typelevel:alleycats-core_2.11:2.0.0", "lang": "scala", "sha1": "a56245e3edd7fe549d4bff058a3851e1f9af754e", "sha256": "88564548103dbed0b8e164e7b6db90514a026edd8eb7511ec2878bad162c6659", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/typelevel/alleycats-core_2.11/2.0.0/alleycats-core_2.11-2.0.0.jar", "source": {"sha1": "6db260bd37e4c1355d4c1ded5213d4e7f28f820c", "sha256": "a2e362f95c7a8c2138d4d7c876dcc811c7d25b3fd5d375194edee29b4644702d", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/typelevel/alleycats-core_2.11/2.0.0/alleycats-core_2.11-2.0.0-sources.jar"} , "name": "org_typelevel_alleycats_core_2_11", "actual": "@org_typelevel_alleycats_core_2_11//jar:file", "bind": "jar/org/typelevel/alleycats_core_2_11"},
    {"artifact": "org.typelevel:catalysts-macros_2.11:0.0.5", "lang": "scala", "sha1": "338c6a322095708d3b198074b016ab8883f0eda9", "sha256": "b9775c48986f5c45b4730c3c77b3ac1a8bc3b697edcedf47f71330be423f808e", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/typelevel/catalysts-macros_2.11/0.0.5/catalysts-macros_2.11-0.0.5.jar", "source": {"sha1": "e68e763b209b19b5fa177fc995d2e6ed4e20a286", "sha256": "a7abcd516b273dc6e1c55c59219bcf9658728fcf6894b9fd65ed67c825265507", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/typelevel/catalysts-macros_2.11/0.0.5/catalysts-macros_2.11-0.0.5-sources.jar"} , "name": "org_typelevel_catalysts_macros_2_11", "actual": "@org_typelevel_catalysts_macros_2_11//jar:file", "bind": "jar/org/typelevel/catalysts_macros_2_11"},
    {"artifact": "org.typelevel:catalysts-platform_2.11:0.0.5", "lang": "scala", "sha1": "da044b8d07c8d1c032bd18a2edbba857f2ea7cd6", "sha256": "d19eb31634821daddabc9b94b98d553b144c34636777ada627350ada8df630bc", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/typelevel/catalysts-platform_2.11/0.0.5/catalysts-platform_2.11-0.0.5.jar", "source": {"sha1": "26fc58cba5ccd1d3f6fad4f11831dd152f9751a0", "sha256": "b647b1fb17ed835c6c8534f64bee2acf0945488565f55f12bae36723444a2b96", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/typelevel/catalysts-platform_2.11/0.0.5/catalysts-platform_2.11-0.0.5-sources.jar"} , "name": "org_typelevel_catalysts_platform_2_11", "actual": "@org_typelevel_catalysts_platform_2_11//jar:file", "bind": "jar/org/typelevel/catalysts_platform_2_11"},
# duplicates in org.typelevel:cats-core_2.11 fixed to 2.0.0
# - com.monovore:decline_2.11:1.0.0 wanted version 2.0.0
# - org.bykn:fastparse-cats-core_2.11:0.1.0 wanted version 1.1.0
# - org.typelevel:alleycats-core_2.11:2.0.0 wanted version 2.0.0
# - org.typelevel:cats-effect_2.11:2.0.0 wanted version 2.0.0
# - org.typelevel:cats-free_2.11:2.0.0 wanted version 2.0.0
# - org.typelevel:cats-laws_2.11:1.1.0 wanted version 1.1.0
# - org.typelevel:cats-testkit_2.11:1.1.0 wanted version 1.1.0
    {"artifact": "org.typelevel:cats-core_2.11:2.0.0", "lang": "scala", "sha1": "332131f129cec93da9d7c27b2ac377c7a7b3a823", "sha256": "ce2ecbeee121ef1746fbf2cf23bc34dfac8fbdb0f9e616aa47ec815b9b117b11", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/typelevel/cats-core_2.11/2.0.0/cats-core_2.11-2.0.0.jar", "source": {"sha1": "8a4985d05e30b72ca3aa7f92db56551f29976445", "sha256": "24a288155dad26d223c66219fef86ada15c0e1ea506f9084f3b3505888781426", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/typelevel/cats-core_2.11/2.0.0/cats-core_2.11-2.0.0-sources.jar"} , "name": "org_typelevel_cats_core_2_11", "actual": "@org_typelevel_cats_core_2_11//jar:file", "bind": "jar/org/typelevel/cats_core_2_11"},
    {"artifact": "org.typelevel:cats-effect_2.11:2.0.0", "lang": "scala", "sha1": "8eb24265e31342a6123f7327bb8f968851ee6237", "sha256": "dc417085a8ab047917f5e24a292a9f592b21b7203a78b5bcebddc189dd36c8a9", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/typelevel/cats-effect_2.11/2.0.0/cats-effect_2.11-2.0.0.jar", "source": {"sha1": "aef925ad69d0e88a4411865d41d1c7aa0644753a", "sha256": "cbe8f1bc549400d6bd3912e2b65319ababbdbd7e3d72f31970f350f279d37e89", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/typelevel/cats-effect_2.11/2.0.0/cats-effect_2.11-2.0.0-sources.jar"} , "name": "org_typelevel_cats_effect_2_11", "actual": "@org_typelevel_cats_effect_2_11//jar:file", "bind": "jar/org/typelevel/cats_effect_2_11"},
    {"artifact": "org.typelevel:cats-free_2.11:2.0.0", "lang": "scala", "sha1": "7d9ba822bbf308ed08640b8aa7f853cd03099f1e", "sha256": "e9be690acde5b51997590118df861cb29f7627a18653ee766c2b4f9000ab69e6", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/typelevel/cats-free_2.11/2.0.0/cats-free_2.11-2.0.0.jar", "source": {"sha1": "9d81a2e05eeb8123a81f9e9791d0b81f99b46560", "sha256": "e9f47dfa5d0f3c044b6e8a8c8388a1207b272860dd240b49bf9e0f8751d6263d", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/typelevel/cats-free_2.11/2.0.0/cats-free_2.11-2.0.0-sources.jar"} , "name": "org_typelevel_cats_free_2_11", "actual": "@org_typelevel_cats_free_2_11//jar:file", "bind": "jar/org/typelevel/cats_free_2_11"},
    {"artifact": "org.typelevel:cats-kernel-laws_2.11:1.1.0", "lang": "scala", "sha1": "fdde23a23376dbcc55d54ea2791fc0f551a1301e", "sha256": "5617d58958e4c477f6f4c54b05d5d8ba5dd3acbcdcbb1a3564d5526ba714184d", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/typelevel/cats-kernel-laws_2.11/1.1.0/cats-kernel-laws_2.11-1.1.0.jar", "source": {"sha1": "87be28566625d0abf084ea263df60d26478617b7", "sha256": "913f3ac5034bc35651380e28eccd2c535fc507587e67ca738dbbcb736bbb0de3", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/typelevel/cats-kernel-laws_2.11/1.1.0/cats-kernel-laws_2.11-1.1.0-sources.jar"} , "name": "org_typelevel_cats_kernel_laws_2_11", "actual": "@org_typelevel_cats_kernel_laws_2_11//jar:file", "bind": "jar/org/typelevel/cats_kernel_laws_2_11"},
# duplicates in org.typelevel:cats-kernel_2.11 fixed to 2.0.0
# - org.typelevel:cats-core_2.11:2.0.0 wanted version 2.0.0
# - org.typelevel:cats-kernel-laws_2.11:1.1.0 wanted version 1.1.0
# - org.typelevel:cats-laws_2.11:1.1.0 wanted version 1.1.0
    {"artifact": "org.typelevel:cats-kernel_2.11:2.0.0", "lang": "scala", "sha1": "97af347bee5d175c485d5f5b743d19d5c3a41e11", "sha256": "578f32499628ea2d80bce00ab2483d17657d0ef0eb6309801c548e7736b430f2", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/typelevel/cats-kernel_2.11/2.0.0/cats-kernel_2.11-2.0.0.jar", "source": {"sha1": "9102f7e966c7178774c3014b6f5f55f0fd47036a", "sha256": "7daf6e85e63ce41de0533f1e2e7e6dedaf71ef795740928c0f05d8c4ed67b975", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/typelevel/cats-kernel_2.11/2.0.0/cats-kernel_2.11-2.0.0-sources.jar"} , "name": "org_typelevel_cats_kernel_2_11", "actual": "@org_typelevel_cats_kernel_2_11//jar:file", "bind": "jar/org/typelevel/cats_kernel_2_11"},
    {"artifact": "org.typelevel:cats-laws_2.11:1.1.0", "lang": "scala", "sha1": "f686924fa60ab0fa214f75b2f44c65223a6e178d", "sha256": "fc4974d4b3b0047832a6ffe7886fea1a328ca530b4984cf35a36606e7ce6667f", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/typelevel/cats-laws_2.11/1.1.0/cats-laws_2.11-1.1.0.jar", "source": {"sha1": "ad5a2190898f77050411f1b4758b93919885570d", "sha256": "897395d7f96dda700822ce47ec93fbf9eeea005178aa1c7e23e0c23bbe874189", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/typelevel/cats-laws_2.11/1.1.0/cats-laws_2.11-1.1.0-sources.jar"} , "name": "org_typelevel_cats_laws_2_11", "actual": "@org_typelevel_cats_laws_2_11//jar:file", "bind": "jar/org/typelevel/cats_laws_2_11"},
# duplicates in org.typelevel:cats-macros_2.11 fixed to 2.0.0
# - org.typelevel:cats-core_2.11:2.0.0 wanted version 2.0.0
# - org.typelevel:cats-free_2.11:2.0.0 wanted version 2.0.0
# - org.typelevel:cats-laws_2.11:1.1.0 wanted version 1.1.0
# - org.typelevel:cats-testkit_2.11:1.1.0 wanted version 1.1.0
    {"artifact": "org.typelevel:cats-macros_2.11:2.0.0", "lang": "scala", "sha1": "1b7915597e3f728c1e8f541075666a7b51227bd4", "sha256": "4fbe50e24da0565193a65175e74160e8c4b3fe28682c94b92db949273ba7b81d", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/typelevel/cats-macros_2.11/2.0.0/cats-macros_2.11-2.0.0.jar", "source": {"sha1": "5dabdab9f8d57f747d42f936dab13302ecb1c974", "sha256": "4765e994e0b07bdb09cb6de413bc4ab2bcee12e14e4a888c76563c616b1149e1", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/typelevel/cats-macros_2.11/2.0.0/cats-macros_2.11-2.0.0-sources.jar"} , "name": "org_typelevel_cats_macros_2_11", "actual": "@org_typelevel_cats_macros_2_11//jar:file", "bind": "jar/org/typelevel/cats_macros_2_11"},
    {"artifact": "org.typelevel:cats-testkit_2.11:1.1.0", "lang": "scala", "sha1": "b5cfd2d79fb308bd4efb32164af67bc965be1d60", "sha256": "91ff7623bba52e683c0be85388ad1ef4e2e3a5349fbd7b164b6a73596fc46646", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/typelevel/cats-testkit_2.11/1.1.0/cats-testkit_2.11-1.1.0.jar", "source": {"sha1": "4ef026a517ffbd113eee304e0a3d6a5290eae152", "sha256": "319e3c572f0eedce7ec46682a5718107ab1318eda6e008c76bcd178bf18413e3", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/typelevel/cats-testkit_2.11/1.1.0/cats-testkit_2.11-1.1.0-sources.jar"} , "name": "org_typelevel_cats_testkit_2_11", "actual": "@org_typelevel_cats_testkit_2_11//jar:file", "bind": "jar/org/typelevel/cats_testkit_2_11"},
    {"artifact": "org.typelevel:discipline_2.11:0.8", "lang": "scala", "sha1": "679ccac1214a393de50ad539aab037646d8037d8", "sha256": "d704d06456dacb1f68d88578227c89c44c7863ec3ea4ef289f35ce4a5e548205", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/typelevel/discipline_2.11/0.8/discipline_2.11-0.8.jar", "source": {"sha1": "8c302d141aa6db6f009a3fcc253669f4685116b1", "sha256": "0eef014e00d756401603e151bef160eb00c596b06951709e4dfc19d6f43fdf04", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/typelevel/discipline_2.11/0.8/discipline_2.11-0.8-sources.jar"} , "name": "org_typelevel_discipline_2_11", "actual": "@org_typelevel_discipline_2_11//jar:file", "bind": "jar/org/typelevel/discipline_2_11"},
    {"artifact": "org.typelevel:machinist_2.11:0.6.2", "lang": "scala", "sha1": "029c6a46d66b6616f8795a70753e6753975f42fc", "sha256": "44d11274e9cf1d6d22cd79a38abd60986041eb8a58083682df29bafd0aaba965", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/typelevel/machinist_2.11/0.6.2/machinist_2.11-0.6.2.jar", "source": {"sha1": "98edae0ef106ad778b87080ef59df8ac08ab4d63", "sha256": "35b143492371211bcc5b9b0b0f4dc1d57ac63b79004124b9ebca7662215f5e89", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/typelevel/machinist_2.11/0.6.2/machinist_2.11-0.6.2-sources.jar"} , "name": "org_typelevel_machinist_2_11", "actual": "@org_typelevel_machinist_2_11//jar:file", "bind": "jar/org/typelevel/machinist_2_11"},
    {"artifact": "org.typelevel:macro-compat_2.11:1.1.1", "lang": "scala", "sha1": "0cb87cb74fd5fb118fede3f98075c2044616b35d", "sha256": "5200a80ad392f0b882021d6de2efb17b874cc179ff8539f9bcedabc100b7890b", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/typelevel/macro-compat_2.11/1.1.1/macro-compat_2.11-1.1.1.jar", "source": {"sha1": "363f86f631e1e95fc7989f73a0cea3ee18107cea", "sha256": "4e3438277b20cd64bce0ba31ffc7b8a74da914551c9dea46297508f879a6f220", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/typelevel/macro-compat_2.11/1.1.1/macro-compat_2.11-1.1.1-sources.jar"} , "name": "org_typelevel_macro_compat_2_11", "actual": "@org_typelevel_macro_compat_2_11//jar:file", "bind": "jar/org/typelevel/macro_compat_2_11"},
    {"artifact": "org.typelevel:paiges-core_2.11:0.3.0", "lang": "scala", "sha1": "b7290c86e6c9281c9cb2534a0d4b26149618b3b2", "sha256": "fa697cb6d1e03cb143183c45cc543734e7600dcb4dee63005738d28a722c202e", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/typelevel/paiges-core_2.11/0.3.0/paiges-core_2.11-0.3.0.jar", "source": {"sha1": "8e9844b1da4e15e2c1c27bfed454f695f86232b5", "sha256": "d678e290975f4e408592c7b80f4e5c375ce122a5526f9cf52daee7bc23387da4", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/typelevel/paiges-core_2.11/0.3.0/paiges-core_2.11-0.3.0-sources.jar"} , "name": "org_typelevel_paiges_core_2_11", "actual": "@org_typelevel_paiges_core_2_11//jar:file", "bind": "jar/org/typelevel/paiges_core_2_11"},
    ]

def maven_dependencies(callback = jar_artifact_callback):
    for hash in list_dependencies():
        callback(hash)