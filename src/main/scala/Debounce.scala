import chisel3._
import chisel3.util._

class Debounce(fac: Int = 100000000/100) extends Module {
  val io = IO(new Bundle {
    val btnU = Input(Bool())
    val sw = Input(UInt(8.W))
    val led = Output(UInt(8.W))
  })

  val btn = io.btnU

  //- start input_sync
  val btnSync = RegNext(RegNext(btn))
  //- end

  /*
  val FAC = 100000000/100
  */

  val FAC = fac

  val btnDeb = Reg(Bool())

  val cntReg = RegInit(0.U(32.W))
  val tick = cntReg === (FAC-1).U

  cntReg := cntReg + 1.U
  when (tick) {
    cntReg := 0.U
    btnDeb := btnSync
  }

  val shiftReg = RegInit(0.U(3.W))
  when (tick) {
    // shift left and input in LSB
    shiftReg := Cat(shiftReg(1, 0), btnDeb)
  }
  // Majority voiting
  val btnFilter = (shiftReg(2) & shiftReg(1)) | (shiftReg(2) & shiftReg(0)) | (shiftReg(1) & shiftReg(0))

  val btnClean = btnFilter

  val risingEdge = btnClean & !RegNext(btnClean)

  // Use the rising edge of the debounced button
  // to count up
  val r1 = RegInit(0.U(8.W))
  when (risingEdge) {
    r1 := r1 + 1.U
  }

  io.led := r1
}

class DebounceFunc(fac: Int = 100000000/100) extends Module {
  val io = IO(new Bundle {
    val btnU = Input(Bool())
    val sw = Input(UInt(8.W))
    val led = Output(UInt(8.W))
  })

  val btn = io.btnU

  def sync(v: Bool) = RegNext(RegNext(v))

  def rising(v: Bool) = v & !RegNext(v)

  def tickGen(fac: Int) = {
    val reg = RegInit(0.U(log2Up(fac).W))
    val tick = reg === (fac-1).U
    reg := Mux(tick, 0.U, reg + 1.U)
    tick
  }

  def filter(v: Bool, t: Bool) = {
    val reg = RegInit(0.U(3.W))
    when (t) {
      reg := Cat(reg(1, 0), v)
    }
    (reg(2) & reg(1)) | (reg(2) & reg(0)) | (reg(1) & reg(0))
  }

  val btnSync = sync(btn)

  /*
  val FAC = 100000000/100
  */

  val FAC = fac
  val tick = tickGen(FAC)

  val btnDeb = Reg(Bool())
  when (tick) {
    btnDeb := btnSync
  }

  val btnFilter = filter(btnDeb, tick)

  val btnClean = btnFilter

  val risingEdge = rising(btnClean)

  // Use the rising edge of the debounced button
  // to count up
  val r1 = RegInit(0.U(8.W))
  when (risingEdge) {
    r1 := r1 + 1.U
  }

  io.led := r1
}

object Debounce extends App {
  println("Generating the Debounce hardware")
  chisel3.Driver.execute(Array("--target-dir", "generated"), () => new Debounce())
}