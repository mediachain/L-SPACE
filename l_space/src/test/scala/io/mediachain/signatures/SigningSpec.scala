package io.mediachain.signatures


import cats.data.Xor
import io.mediachain.Types.Canonical
import io.mediachain.{GraphFixture, XorMatchers}
import io.mediachain.core.TranslationError.InvalidFormat
import org.specs2.Specification

object SigningSpec extends Specification
  with XorMatchers  {

  def is =
  s2"""
  - Signs a text string $signsText
  - Does not validate signature for modified text $doesNotValidateModifiedText
  - Signs a String containing json text, first converting to canonical CBOR $signsJsonText
  - Signs a Canonical case class $signsCanonical
  - Validates a signed Canonical with an X509 certificate $validatesWithCertificate
  - Validates a signed PhotoBlob with a certificate store $validatesWithCertificateStore
      """

  lazy val certificateStoreRootDirPath = getClass.getResource("/certificates")
    .toURI.getPath

  lazy val certificateStore =
    LocalCertificateStore.getInstance(Some(certificateStoreRootDirPath))

  lazy val privateKeyStream = getClass.getResourceAsStream(
    "/private-keys/lspace.mine.nyc/private-key.pem")


  lazy val privateKey =
    PEMFileUtil.privateKeyFromInputStream(privateKeyStream)
    .getOrElse(throw new IllegalStateException(
      "Can't read testing private key from classpath"))

  lazy val cert =
    certificateStore.certificateForCommonName("lspace.mine.nyc")
    .getOrElse(throw new IllegalStateException(
      "Can't read testing certificate from local certificate store"))

  lazy val signatory = Signatory("lspace.mine.nyc", privateKey)

  lazy val publicKey = cert.getPublicKey

  def signsText = {
    val text = "Sign me, please!"
    val signature = Signer.signText(text, privateKey)

    val valid = Signer.verifySignedText(text, signature, publicKey)
    valid must beTrue
  }


  def doesNotValidateModifiedText = {
    val text = "Sign me, please!"
    val signature = Signer.signText(text, privateKey)
    val modifiedText = text + " Freemasons run the country!"

    val valid = Signer.verifySignedText(modifiedText, signature, publicKey)
    valid must beFalse
  }


  def signsJsonText = {
    val jsonString = """  {"title": "Harder Better Faster Stronger", "artist": "Daft Punk"} """
    val reorderedJsonString =
      """{
        | "artist": "Daft Punk",
        | "title": "Harder Better Faster Stronger"
        |}""".stripMargin

    val results: Xor[InvalidFormat, (Boolean, Boolean)] =
      for {
        signature <- Signer.signCborRepresentationOfJsonText(jsonString, privateKey)

        originalIsValid <- Signer.verifySignedJsonText(jsonString, signature, publicKey)

        reorderedIsValid <- Signer.verifySignedJsonText(reorderedJsonString, signature, publicKey)
      } yield (originalIsValid, reorderedIsValid)

    results must beRightXor { results =>
      (results._1 must beTrue) and (results._2 must beTrue)
    }
  }

  def signsCanonical = {
    val commonName = "foo.bar.baz"
    val c = Canonical.create().withSignature(commonName, privateKey)
    val sig = c.signatures(commonName)

    Signer.verifySignedSignable(c, sig, publicKey) must beTrue
  }


  def validatesWithCertificate = {
    val result = for {
      commonName <- CertificateUtil.commonName(cert)
      canonical = Canonical.create().withSignature(signatory)
      result <- Signer.validateSignableWithCertificate(canonical, cert)
    } yield result

    result must beRightXor(_ must beTrue)
  }


  def validatesWithCertificateStore = {
    val blob = GraphFixture.Util.getImageBlob.withSignature(signatory)
    val result = Signer.validateSignableWithCertificateStore(blob, certificateStore)

    result must beRightXor(_ must beTrue)
  }
}
